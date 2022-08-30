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
          _ <- IO.println("[zzz] starting sleep")
          d <- refDef.get
          _ <- d.get
          _ <- IO.println("[zzz] awaken")
        } yield ()

      def wakeUp: IO[Unit] =
        for {
          _ <- IO.println("[zzz] wake up")
          newd <- Deferred[IO, Unit]
          d <- refDef.get
          _ <- d.complete(())
          _ <- refDef.set(newd)
        } yield ()
    }
}
