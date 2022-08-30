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
    res <- jobqueue.JobScheduler.resource(1)
    rr <- res.use { r =>
      r.schedule(IO.sleep(Duration(1, "second")) *> IO.println("foo"))
      r.schedule(IO.sleep(Duration(1, "second")) *> IO.println("foo"))
      r.schedule(IO.sleep(Duration(1, "second")) *> IO.println("foo"))
    }

    // id <- res.schedule(IO.println("job"))
  } yield ExitCode.Success
}
