package com.github.windymelt.ffq.jobqueue

import cats.effect.IO
import cats.data.Chain
import cats.effect.kernel.Ref
import cats.effect.kernel.Resource
import cats.effect.kernel.Outcome

trait JobScheduler {
  def schedule(task: IO[_]): IO[Job.Id]
}

object JobScheduler {
  case class State(
      maxRunning: Int,
      scheduled: Chain[Job.Scheduled] = Chain.empty,
      running: Map[Job.Id, Job.Running] = Map.empty,
      completed: Chain[Job.Completed] = Chain.empty
  ) {
    def enqueue(job: Job.Scheduled): State = copy(scheduled = scheduled :+ job)
    def dequeue: (State, Option[Job.Scheduled]) = {
      if (running.size >= maxRunning) this -> None
      else
        scheduled.uncons.map { case (head, tail) =>
          copy(scheduled = tail) -> Some(head)
        } getOrElse (this -> None)
    }
    def running(job: Job.Running): State =
      copy(running = running + (job.id -> job))
    def onComplete(job: Job.Completed): State = {
      copy(completed = job +: completed, running = running.tail)
    }
  }

  def resource(maxRunning: Int): IO[Resource[IO, JobScheduler]] =
    for {
      _ <- IO.println("[scheduler] creating scheduler resource...")
      schedulerState <- Ref[IO].of(JobScheduler.State(maxRunning))
      zzz <- Zzz.asleep
      scheduler: JobScheduler = new JobScheduler {
        def schedule(task: IO[_]): IO[Job.Id] =
          for {
            _ <- IO.println("[scheduler] scheduling task...")
            job <- Job.create(task)
            _ <- schedulerState.update(_.enqueue(job))
            _ <- IO.println(s"[scheduler] state upated: enququed ${job.id}")
            _ <- schedulerState.get.flatMap(st =>
              IO.println(
                s"[scheduler] state: scheduled: ${st.scheduled.size} / running: ${st.running.size} / completed: ${st.completed.size}"
              )
            )
            _ <- IO.println("[scheduler] job scheduled. waking up trigger")
            _ <- zzz.wakeUp
          } yield job.id
      }
      reactor = Reactor(schedulerState)
      onStart = (id: Job.Id) => IO.println("[scheduler] starting resource")
      onComplete = (id: Job.Id, exitCase: Outcome[IO, Throwable, Unit]) =>
        for {
          _ <- IO.println("[scheduler] completed job; waking up trigger")
          st <- schedulerState.get
          _ <- IO.println(
            s"            state: scheduled: ${st.scheduled.size} / running: ${st.running.size} / completed: ${st.completed.size}"
          )
          _ <- zzz.wakeUp
        } yield ()
      loop: IO[Nothing] = (zzz.sleep *> reactor.whenAwake(
        onStart,
        onComplete
      )).foreverM
    } yield loop.background.map(_ => scheduler) // loop.background.as(scheduler)
}
