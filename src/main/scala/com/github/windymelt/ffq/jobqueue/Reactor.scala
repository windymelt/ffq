package com.github.windymelt.ffq.jobqueue

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Outcome

trait Reactor {
  def whenAwake(
      onStart: Job.Id => IO[Unit],
      onComplete: (Job.Id, Outcome[IO, Throwable, Unit]) => IO[Unit]
  ): IO[Unit]
}

object Reactor {
  def apply(stateRef: Ref[IO, JobScheduler.State]): Reactor =
    new Reactor {
      def whenAwake(
          onStart: Job.Id => IO[Unit],
          onComplete: (Job.Id, Outcome[IO, Throwable, Unit]) => IO[Unit]
      ): IO[Unit] = {
        def startNextJob: IO[Option[Job.Running]] = {
          import cats.implicits._
          for {
            job <- stateRef.modify(_.dequeue)
            running <- job.traverse(startJob)
          } yield running
        }

        def startJob(scheduled: Job.Scheduled): IO[Job.Running] =
          for {
            _ <- IO.println("[reactor] starting asynchronous job...")
            running <- scheduled.start
            _ <- stateRef.update(_.running(running))
            _ <- stateRef.get.flatMap(st =>
              IO.println(s"current running job: ${st.running.size}")
            )
            _ <- registerOnComplete(running)
            _ <- onStart(running.id).attempt
          } yield running

        def registerOnComplete(job: Job.Running) =
          job.await.flatMap(jobCompleted).start

        def jobCompleted(job: Job.Completed): IO[Unit] =
          for {
            _ <- IO.println(s"[reactor] completed job ${job.id}")
            _ <- stateRef
              .update(_.onComplete(job))
              .flatTap(_ => onComplete(job.id, job.exitCase).attempt)
          } yield ()

        startNextJob
          .iterateUntil(_.isEmpty)
          .void
      }
    }
}
