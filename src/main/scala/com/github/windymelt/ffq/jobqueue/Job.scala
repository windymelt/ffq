package com.github.windymelt.ffq.jobqueue

import java.util.UUID
import cats.effect.IO
import cats.effect.kernel.Fiber
import cats.effect.kernel.Deferred
import cats.effect.kernel.Outcome

sealed trait Job {}

object Job {
  case class Id(value: UUID) extends AnyVal

  case class Scheduled(id: Id, task: IO[_]) extends Job {
    def start(): IO[Job.Running] = for {
      exitCase <- Deferred[IO, Outcome[IO, Throwable, Unit]]
      fiber <- task.void.guaranteeCase(a => exitCase.complete(a).void).start
    } yield Job.Running(id, fiber, exitCase)
  }
  case class Running(
      id: Id,
      fiber: Fiber[IO, Throwable, Unit],
      exitCase: Deferred[IO, Outcome[IO, Throwable, Unit]]
  ) extends Job {
    val await: IO[Completed] = exitCase.get.map(Completed(id, _))
  }
  case class Completed(id: Id, exitCase: Outcome[IO, Throwable, Unit])
      extends Job

  def create[A](task: IO[A]): IO[Scheduled] =
    IO(Id(UUID.randomUUID())).map(Scheduled(_, task))
}
