package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.{PaymentType, Program, ProgramFilter, Trainer}
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import com.misterjvm.reviewboard.repositories.{ProgramRepository, TrainerRepository, TrainerRepositoryLive}
import com.misterjvm.reviewboard.syntax.*
import zio.*
import zio.test.*

object ProgramServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[ProgramService]

  val stubRepoLayer = ZLayer.succeed(
    new ProgramRepository {
      val db = collection.mutable.Map[Long, Program]()

      def create(program: Program): Task[Program] =
        ZIO.succeed {
          val nextId     = db.keys.maxOption.getOrElse(0L) + 1
          val newProgram = program.copy(id = nextId)
          db += (nextId -> newProgram)
          newProgram
        }

      def getById(id: Long): Task[Option[Program]] =
        ZIO.succeed(db.get(id))

      def getBySlug(slug: String): Task[Option[Program]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      def get: Task[List[Program]] =
        ZIO.succeed(db.values.toList)

      def update(id: Long, op: Program => Program): Task[Program] =
        ZIO.attempt {
          val program = db(id)
          db += (id -> op(program))
          program
        }

      def delete(id: Long): Task[Program] =
        ZIO.attempt {
          val program = db(id)
          db -= id
          program
        }

      def uniqueAttributes: Task[ProgramFilter] =
        ZIO.succeed(ProgramFilter.empty)

      def search(filter: ProgramFilter): Task[List[Program]] =
        ZIO.succeed(List())
    }
  )

  val stubTrainerRepoLayer = ZLayer.succeed {
    new TrainerRepository {
      override def getById(id: Long): Task[Option[Trainer]] =
        ZIO.succeed(Some(Trainer(1L, "trainer", "description", "https://trainer.com", None)))
    }
  }

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ProgramServiceSpec")(
      test("create") {
        val programZIO =
          service(_.create(CreateProgramRequest("PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess)))
        programZIO.assert { program =>
          program.name == "PJF Performance" &&
          program.url == "pjf.com" &&
          program.slug == "pjf-performance"
        }
      },
      test("getById") {
        val programZIO = for {
          program <- service(
            _.create(CreateProgramRequest("PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess))
          )
          programOpt <- service(_.getById(program.id))
        } yield (program, programOpt)

        programZIO.assert {
          case (program, Some(programRes)) =>
            program.name == "PJF Performance" &&
            program.url == "pjf.com" &&
            program.slug == "pjf-performance" &&
            program == programRes
          case _ => false
        }
      },
      test("getBySlug") {
        val programZIO = for {
          program <- service(
            _.create(CreateProgramRequest("PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess))
          )
          programOpt <- service(_.getBySlug(program.slug))
        } yield (program, programOpt)

        programZIO.assert {
          case (program, Some(programRes)) =>
            program.name == "PJF Performance" &&
            program.url == "pjf.com" &&
            program.slug == "pjf-performance" &&
            program == programRes
          case _ => false
        }
      },
      test("get") {
        val programZIO = for {
          program <- service(
            _.create(CreateProgramRequest("PJF Performance", "pjf.com", 1, PaymentType.LifetimeAccess))
          )
          program2 <- service(
            _.create(CreateProgramRequest("PJF Performance2", "pjf2.com", 2, PaymentType.Subscription))
          )
          programs <- service(_.getAll)
        } yield (program, program2, programs)

        programZIO.assert { case (program, program2, programs) =>
          programs.toSet == Set(program, program2)
        }
      }
    ).provide(ProgramServiceLive.layer, stubRepoLayer, stubTrainerRepoLayer)
}
