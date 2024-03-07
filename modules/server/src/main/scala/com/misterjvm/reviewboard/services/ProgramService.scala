package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import com.misterjvm.reviewboard.repositories.ProgramRepository
import zio.*

import scala.collection.mutable

// BUSINESS LOGIC
// in between the HTTP layer and the DB layer
trait ProgramService {
  def create(request: CreateProgramRequest): Task[Program]
  def getAll: Task[List[Program]]
  def getById(id: Long): Task[Option[Program]]
  def getBySlug(slug: String): Task[Option[Program]]
  def allFilters: Task[ProgramFilter]
}

class ProgramServiceLive private (repo: ProgramRepository) extends ProgramService {
  override def create(request: CreateProgramRequest): Task[Program] =
    repo.create(request.toProgram(-1L))

  override def getAll: Task[List[Program]] =
    repo.get

  override def getById(id: Long): Task[Option[Program]] =
    repo.getById(id)

  override def getBySlug(slug: String): Task[Option[Program]] =
    repo.getBySlug(slug)

  override def allFilters: Task[ProgramFilter] =
    repo.uniqueAttributes
}

object ProgramServiceLive {
  val layer = ZLayer {
    for {
      repo <- ZIO.service[ProgramRepository]
    } yield new ProgramServiceLive(repo)
  }
}
