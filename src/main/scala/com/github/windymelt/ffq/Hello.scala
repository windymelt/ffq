package com.github.windymelt.ffq

import cats.effect.IOApp
import cats.effect.IO
import cats.effect.ExitCode
import cats.effect.kernel.Ref
import com.github.windymelt.ffq.jobqueue.JobScheduler
import scala.concurrent.duration.Duration

object Hello extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- IO.println("running main")
    res <- jobqueue.JobScheduler.resource(10)
    rr <- res.use { r =>
      for {
        _ <- r.schedule(IO.sleep(Duration(1, "second")) *> IO.println("foo"))
        _ <- r.schedule(IO.sleep(Duration(1, "second")) *> IO.println("bar"))
        _ <- r.schedule(IO.sleep(Duration(1, "second")) *> IO.println("buzz"))
        _ <- IO.sleep(Duration(5, "seconds"))
      } yield ()
    }

    // id <- res.schedule(IO.println("job"))
  } yield ExitCode.Success
}
