package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.domain.data.*
import com.misterjvm.reviewboard.http.requests.CreateProgramRequest
import zio.*

import scala.collection.mutable

// BUSINESS LOGIC
// in between the HTTP layer and the DB layer
trait ProgramService {
  def create(request: CreateProgramRequest): Task[Program]
  def getAll: Task[List[Program]]
  def getById(id: Long): Task[Option[Program]]
  def getBySlug(slug: String): Task[Option[Program]]
}

object ProgramService {
  val dummyLayer = ZLayer.succeed(new ProgramServiceDummy)
}

class ProgramServiceDummy extends ProgramService {
  val db = mutable.Map[Long, Program]()

  override def create(request: CreateProgramRequest): Task[Program] =
    ZIO.succeed {
      val newId      = db.keys.maxOption.getOrElse(0L) + 1
      val newProgram = request.toProgram(newId)
      db += (newId -> newProgram)
      newProgram
    }

  override def getAll: Task[List[Program]] =
    ZIO.succeed(db.values.toList)

  override def getById(id: Long): Task[Option[Program]] =
    ZIO.succeed(db.get(id))

  override def getBySlug(slug: String): Task[Option[Program]] =
    ZIO.succeed(db.values.find(_.slug == slug))
}
