package com.misterjvm.reviewboard.services

import com.misterjvm.reviewboard.config.{Configs, SummaryConfig}
import com.misterjvm.reviewboard.domain.data.{Review, ReviewSummary}
import com.misterjvm.reviewboard.http.requests.CreateReviewRequest
import com.misterjvm.reviewboard.repositories.ReviewRepository
import zio.*

import java.time.Instant

trait ReviewService {
  def create(request: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByProgramId(programId: Long): Task[List[Review]]
  def getByProgramSlug(programSlug: String): Task[List[Review]]
  def getByUserId(userId: Long): Task[List[Review]]
  def getSummary(programId: Long): Task[Option[ReviewSummary]]
  def makeSummary(programId: Long): Task[Option[ReviewSummary]]
}

class ReviewServiceLive private (repo: ReviewRepository, openAIService: OpenAIService, config: SummaryConfig)
    extends ReviewService {
  override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(
      Review(
        id = -1L,
        programId = request.programId,
        programSlug = request.programSlug,
        userId = userId,
        value = request.value,
        quality = request.quality,
        content = request.content,
        userExperience = request.userExperience,
        accessibility = request.accessibility,
        support = request.support,
        wouldRecommend = request.wouldRecommend,
        review = request.review,
        created = Instant.now(),
        updated = Instant.now()
      )
    )

  override def getById(id: Long): Task[Option[Review]] =
    repo.getById(id)

  override def getByProgramId(programId: Long): Task[List[Review]] =
    repo.getByProgramId(programId)

  override def getByProgramSlug(programSlug: String): Task[List[Review]] =
    repo.getByProgramSlug(programSlug)

  override def getByUserId(userId: Long): Task[List[Review]] =
    repo.getByUserId(userId)

  override def getSummary(programId: Long): Task[Option[ReviewSummary]] =
    repo.getSummary(programId)

  override def makeSummary(programId: Long): Task[Option[ReviewSummary]] =
    getByProgramId(programId)
      .flatMap(list => Random.shuffle(list))
      .map(_.take(config.nSelected))
      .flatMap { reviews =>
        val currentSummary: Task[Option[String]] =
          if (reviews.size < config.minReviews)
            ZIO.succeed(Some(s"Need to have at least ${config.minReviews} reviews to generate a summary."))
          else
            buildPrompt(reviews).flatMap(openAIService.getCompletion)

        currentSummary.flatMap {
          case None          => ZIO.none
          case Some(summary) => repo.insertSummary(programId, summary).map(Some(_))
        }
      }

  private def buildPrompt(reviews: List[Review]): Task[String] = ZIO.succeed {
    s"""
    You have the following reviews about a basketball training program:

    ${reviews.zipWithIndex
        .map {
          case (
                Review(
                  _,
                  _,
                  _,
                  _,
                  value,
                  quality,
                  content,
                  userExperience,
                  accessibility,
                  support,
                  wouldRecommend,
                  review,
                  _,
                  _
                ),
                index
              ) =>
            s"""
          Review ${index + 1}:
            Value: ${value.score} stars / 5
            Quality: ${quality.score} stars / 5
            Content: ${content.score} stars / 5
            User Experience: ${userExperience.score} stars / 5
            Accessibility: ${accessibility.score} stars / 5
            Support: ${support.score} stars / 5
            Net promoter score: ${wouldRecommend.score} stars / 5
            Content: "$review"
          """
        }
        .mkString("\n")}

    Make a summary of all these reviews in at most one paragraph.
    """
  }
}

object ReviewServiceLive {
  val layer = ZLayer {
    for {
      repo          <- ZIO.service[ReviewRepository]
      openAIService <- ZIO.service[OpenAIService]
      config        <- ZIO.service[SummaryConfig]
    } yield new ReviewServiceLive(repo, openAIService, config)
  }

  val configuredLayer =
    Configs.makeLayer[SummaryConfig]("misterjvm.summaries") >>> layer
}
