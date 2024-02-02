package com.misterjvm

import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.*
import zio.http.Server
import zio.json.{DeriveJsonCodec, JsonCodec}

import scala.collection.mutable

object TapirDemo extends ZIOAppDefault {

  val simplestEndpoint = endpoint
    .tag("simple")
    .name("simple")
    .description("simplest endpoint possible")
    // ^^ for documentation
    .get                    // http method
    .in("simple")           // path
    .out(plainBody[String]) // output
    .serverLogicSuccess[Task](_ => ZIO.succeed("All good!"))

  val simpleServerProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g. CORS
    ).toHttp(simplestEndpoint)
  )

  // simulate a job board
  val db: mutable.Map[Long, Job] = mutable.Map(
    1L -> Job(1L, "Instructor", "misterjvm.com", "Rock the JVM")
  )

  // create
  val createEndpoint: ServerEndpoint[Any, Task] = endpoint
    .tag("jobs")
    .name("create")
    .description("Create a job")
    .in("jobs")
    .post
    .in(jsonBody[CreateJobRequest])
    .out(jsonBody[Job])
    .serverLogicSuccess(request =>
      ZIO.succeed {
        // insert a new job in the database
        val newId  = db.keys.max + 1
        val newJob = Job(newId, request.title, request.url, request.company)
        db += (newId -> newJob)
        newJob
      }
    )

  // get by id
  val getByIdEndpoint: ServerEndpoint[Any, Task] =
    endpoint
      .tag("jobs")
      .name("getById")
      .description("Get job by ID")
      .in("jobs" / path[Long]("id"))
      .get
      .out(jsonBody[Option[Job]])
      .serverLogicSuccess(id => ZIO.succeed(db.get(id)))

  // get all
  val getAllEndpoint: ServerEndpoint[Any, Task] {
    type SECURITY_INPUT = Unit; type PRINCIPAL = Unit; type INPUT = Unit; type ERROR_OUTPUT = Unit;
    type OUTPUT         = List[Job]
  } = endpoint
    .tag("jobs")
    .name("getAll")
    .description("Get all jobs")
    .in("jobs")
    .get
    .out(jsonBody[List[Job]])
    .serverLogicSuccess(_ => ZIO.succeed(db.values.toList))

  val serverProgram = Server.serve(
    ZioHttpInterpreter(
      ZioHttpServerOptions.default // can add configs e.g. CORS
    ).toHttp(List(createEndpoint, getByIdEndpoint, getAllEndpoint))
  )

  override def run = serverProgram.provide(Server.default)
}

// special request for the HTTP endpoint
case class CreateJobRequest(
    title: String,
    url: String,
    company: String
)

object CreateJobRequest {
  given codec: JsonCodec[CreateJobRequest] = DeriveJsonCodec.gen[CreateJobRequest]
}
