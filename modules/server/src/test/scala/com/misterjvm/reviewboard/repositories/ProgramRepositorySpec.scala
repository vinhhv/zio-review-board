package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.{DataFixtures, PaymentType, Program, ProgramFilter}
import com.misterjvm.reviewboard.syntax.*
import zio.*
import zio.test.*

import java.sql.SQLException

object ProgramRepositorySpec extends ZIOSpecDefault with RepositorySpec with DataFixtures {
  private val pjf =
    Program(
      1L,
      "pjf-performance",
      "PJF Performance",
      "pjf.com",
      1,
      "Paul J. Fabritz",
      PaymentType.LifetimeAccess,
      None,
      List("Vertical")
    )

  private val pjf2 =
    Program(
      2L,
      "pjf-performance-2",
      "PJF Performance 2",
      "pjf2.com",
      1,
      "Paul R. Fabritz",
      PaymentType.LifetimeAccess,
      None,
      List("Hoops")
    )

  override val initScript: String = "sql/initdb.sql"

  override def spec: Spec[TestEnvironment & Scope, Any] =
    suite("ProgramRepositorySpec")(
      test("create a program") {
        val program = for {
          repo    <- ZIO.service[ProgramRepository]
          program <- repo.create(pjf)
        } yield program

        program.assert {
          case Program(
                _,
                "pjf-performance",
                "PJF Performance",
                "pjf.com",
                1,
                "Paul J. Fabritz",
                PaymentType.LifetimeAccess,
                _,
                _
              ) =>
            true
          case _ => false
        }
      },
      test("create duplicate program failure") {
        val program = for {
          repo <- ZIO.service[ProgramRepository]
          _    <- repo.create(pjf)
          err  <- repo.create(pjf).flip
        } yield err

        program.assert(_.isInstanceOf[SQLException])
      },
      test("getById and getBySlug") {
        val program = for {
          repo          <- ZIO.service[ProgramRepository]
          program       <- repo.create(pjf)
          programById   <- repo.getById(program.id)
          programBySlug <- repo.getBySlug(program.slug)
        } yield (program, programById, programBySlug)

        program.assert { case (programOG, program1, program2) =>
          program1.contains(programOG) && program2.contains(programOG)
        }
      },
      test("update a program") {
        val program = for {
          repo    <- ZIO.service[ProgramRepository]
          program <- repo.create(pjf)
          updated <- repo.update(program.id, _.copy(url = "pjfperformance.net"))
          fetched <- repo.getById(program.id)
        } yield (updated, fetched)

        program.assert { case (updated, fetched) =>
          fetched.contains(updated)
        }
      },
      test("delete a program") {
        val program = for {
          repo    <- ZIO.service[ProgramRepository]
          program <- repo.create(pjf)
          _       <- repo.delete(program.id)
          fetched <- repo.getById(program.id)
        } yield fetched

        program.assert(_.isEmpty)
      },
      test("getAll programs") {
        val program = for {
          repo    <- ZIO.service[ProgramRepository]
          _       <- repo.create(pjf)
          _       <- repo.create(pjf2)
          fetched <- repo.get
        } yield fetched

        program.assert { case (fetched) =>
          fetched.size == 2
        }
      },
      test("search by tag") {
        for {
          repo    <- ZIO.service[ProgramRepository]
          created <- repo.create(pjf)
          fetched <- repo.search(ProgramFilter(tags = created.tags.headOption.toList))
        } yield assertTrue {
          fetched.nonEmpty && fetched.tail.isEmpty && fetched.head == created
        }
      }
    ).provide(ProgramRepositoryLive.layer, dataSourceLayer, Repository.quillLayer, Scope.default)
}
