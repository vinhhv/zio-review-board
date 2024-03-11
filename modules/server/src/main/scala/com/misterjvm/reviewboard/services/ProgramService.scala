package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import com.misterjvm.reviewboard.repositories.ProgramRepository
import zio.*

import scala.collection.mutable
import com.misterjvm.reviewboard.repositories.TrainerRepository

// BUSINESS LOGIC
// in between the HTTP layer and the DB layer
trait ProgramService {
  def create(request: CreateProgramRequest): Task[Program]
  def getAll: Task[List[Program]]
  def getById(id: Long): Task[Option[Program]]
  def getBySlug(slug: String): Task[Option[Program]]
  def allFilters: Task[ProgramFilter]
  def search(filter: ProgramFilter): Task[List[Program]]
}

class ProgramServiceLive private (repo: ProgramRepository, trainerRepo: TrainerRepository) extends ProgramService {
  override def create(request: CreateProgramRequest): Task[Program] =
    for {
      trainer <- trainerRepo
        .getById(request.trainerId)
        .someOrFail(new RuntimeException(s"Could not find trainer ${request.trainerId}"))
      program <- repo.create(request.toProgram(-1L, trainer.name))
    } yield program

  override def getAll: Task[List[Program]] =
    repo.get

  override def getById(id: Long): Task[Option[Program]] =
    repo.getById(id)

  override def getBySlug(slug: String): Task[Option[Program]] =
    repo.getBySlug(slug)

  override def allFilters: Task[ProgramFilter] =
    repo.uniqueAttributes

  override def search(filter: ProgramFilter): Task[List[Program]] =
    repo.search(filter)
}

object ProgramServiceLive {
  val layer = ZLayer {
    for {
      repo        <- ZIO.service[ProgramRepository]
      trainerRepo <- ZIO.service[TrainerRepository]
    } yield new ProgramServiceLive(repo, trainerRepo)
  }
}
