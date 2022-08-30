package com.github.windymelt.ffq.jobqueue

import cats.effect.IO
import cats.effect.kernel.Ref
import cats.effect.kernel.Deferred

trait Zzz {
  def sleep: IO[Unit]
  def wakeUp: IO[Unit]
}

object Zzz {
  def asleep = apply()
  def apply(): IO[Zzz] =
    for {
      init <- Deferred[IO, Unit]
      refDef <- Ref[IO].of(init)
    } yield new Zzz {
      def sleep: IO[Unit] =
        for {
          _ <- refDef.get.map(
            _.get
          ) // caveat: Due to automatic context shift, you should write in single flatmap
        } yield ()

      def wakeUp: IO[Unit] =
        for {
          d <- Deferred[IO, Unit].flatMap(refDef.getAndSet)
          _ <- IO.print(".")
          _ <- d.complete(())
        } yield ()
    }
}
