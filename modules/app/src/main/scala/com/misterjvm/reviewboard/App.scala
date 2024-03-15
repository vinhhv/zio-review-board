package com.misterjvm.reviewboard

import com.misterjvm.reviewboard.components.*
import com.misterjvm.reviewboard.core.Session
import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.airstream.timing.PeriodicStream
import com.raquo.laminar.api.L.{*, given}
import frontroute.LinkHandler
import org.scalajs.dom

import scala.util.Try

object App {

  val app = div(
    onMountCallback(_ => Session.loadUserState()),
    Header(),
    Router(),
    Footer()
  ).amend(LinkHandler.bind) // for internal links

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    render(
      containerNode,
      app
    )
  }
}

// reactive variables
object Tutorial {
  val staticContent =
    div(
      // modifiers
      styleAttr := "color:red", // <div style="color:red">
      p("NothingButNet from Laminar!"),
      p("Hello!")
    )

  // EventStream - produce values of the same type
  val ticks: PeriodicStream[Int] = EventStream.periodic(1000) // EventStream[Int]
  // subscription - Airstream
  // ownership - manages subscription
  val subscription = ticks.addObserver(new Observer[Int] {
    override def onError(err: Throwable): Unit    = ()
    override def onTry(nextValue: Try[Int]): Unit = ()
    override def onNext(nextValue: Int): Unit     = dom.console.log(s"Ticks: $nextValue")
  })(new OneTimeOwner(() => ()))

  scala.scalajs.js.timers.setTimeout(10000)(subscription.kill())

  val timeUpdated =
    div(
      span("Time since loaded: "),
      child <-- ticks.map(number => s"$number seconds")
    )

  // EventBus - like EventStreams, but you can push new elements to the stream
  val clickEvents = EventBus[Int]()
  val clickUpdated = div(
    span("Clicks since loaded: "),
    child <-- clickEvents.events.scanLeft(0)(_ + _).map(number => s"$number clicks"),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    )
  )

  // Signal - similar to EventStreams, but have a "current value" (state)
  // can be inspected for the current state (if Laminar/Airstream knows that it has an owner)
  val countSignal = clickEvents.events.scanLeft(0)(_ + _).observe(new OneTimeOwner(() => ()))
  // val currentState = countSignal.now()
  val queryEvents = EventBus[Unit]()

  val clicksQueried = div(
    span("Clicks since loaded: "),
    child <-- queryEvents.events.map(_ => countSignal.now()),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add a click"
    ),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.mapTo(()) --> queryEvents,
      "Refresh count"
    )
  )

  // Var - reactive variable
  val countVar = Var[Int](0)
  val clicksVar = div(
    span("Clicks so far: "),
    child <-- countVar.signal.map(_.toString),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      "Add a click",
      // onClick --> countVar.updater((current, event) => current + 1)
      // onClick -> countVar.writer.contramap(event => countVar.now() + 1)
      onClick --> (_ => countVar.set(countVar.now() + 1))
    )
  )

  /**           no state         |    with state
   * ----------------------------+---------------------
   * read      EventStream       |    Signal
   * ----------------------------+---------------------
   * write      EventBus         |    Var
   */
}
