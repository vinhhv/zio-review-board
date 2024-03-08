package com.misterjvm.reviewboard.core

import com.misterjvm.reviewboard.domain.data.UserToken
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.scalajs.js.*

object Session {
  val stateName: String                 = "userState"
  val userState: Var[Option[UserToken]] = Var(Option.empty)

  def isActive = userState.now().nonEmpty

  def setUserState(token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set("userState", token)
  }

  def loadUserState(): Unit = {
    // clears any expired token
    Storage
      .get[UserToken](stateName)
      .filter(_.expires * 1000 <= new Date().getTime())
      .foreach(_ => Storage.remove(stateName))

    // retrieve the user token (known to be valid)
    userState.set(
      Storage
        .get[UserToken](stateName)
    )
  }

  def clearUserState(): Unit = {
    Storage.remove(stateName)
    userState.set(Option.empty)
  }
}
