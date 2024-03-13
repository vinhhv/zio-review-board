package com.misterjvm.reviewboard.repositories

import com.misterjvm.reviewboard.domain.data.PaymentType
import com.misterjvm.reviewboard.domain.data.Program
import io.getquill.*
import io.getquill.jdbczio.Quill

trait ProgramMeta(quill: Quill.Postgres[SnakeCase]) {
  import quill.*

  inline given programSchema: SchemaMeta[Program]  = schemaMeta[Program]("programs")
  inline given programInsMeta: InsertMeta[Program] = insertMeta[Program](_.id)
  inline given programUpMeta: UpdateMeta[Program]  = updateMeta[Program](_.id, _.trainerId)
}
