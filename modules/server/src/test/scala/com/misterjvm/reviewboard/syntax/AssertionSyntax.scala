package com.misterjvm.reviewboard.syntax

import zio.*
import zio.test.*

extension [R, E, A](zio: ZIO[R, E, A])
  def assert(assertion: Assertion[A]): ZIO[R, E, TestResult] =
    assertZIO(zio)(assertion)
  def assert(predicate: (=> A) => Boolean): ZIO[R, E, TestResult] =
    assert(Assertion.assertion("test assertion")(predicate))
  def assert(name: String)(predicate: (=> A) => Boolean): ZIO[R, E, TestResult] =
    assert(Assertion.assertion(name)(predicate))
