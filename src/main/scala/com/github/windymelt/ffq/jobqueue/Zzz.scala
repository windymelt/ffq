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
          _ <- IO.println("[zzz]sleeping")
          d <- renew
          _ <- d.get
        } yield ()

      val renew = for {
        d <- refDef.get
        tg <- d.tryGet
        d2 <- tg match {
          case None    => IO.pure(d) // not completed
          case Some(_) => Deferred[IO, Unit] // renew
        }
        d3 <- refDef.set(d2)
      } yield d2

      def wakeUp: IO[Unit] =
        for {
          _ <- IO.println("[zzz] waking up")
          d <- Deferred[IO, Unit].flatMap(refDef.getAndSet)
          _ <- d.complete(())
        } yield ()
    }
}
