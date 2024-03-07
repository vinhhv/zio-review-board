package com.misterjvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.*
import org.scalajs.dom
import zio.*
import com.misterjvm.reviewboard.domain.data.ProgramFilter
import com.misterjvm.reviewboard.core.ZJS.*
import com.misterjvm.reviewboard.core.ZJS

/**
  * 1. Populate the panel with the right values
  *   a. expose some API that will retrieve the unique values for filtering
  *   b. fetch those values to populate the panel
  */

/**
  * 2. Update the FilterPanel when they interact with it
  * 3. When clicking the "apply filters" we should retrieve just those programs
  *   a. make the backend search
  *   b. refetch programs when user clicks the filter
  */
object FilterPanel {

  val GROUP_TRAINERS      = "Locations"
  val GROUP_PAYMENT_TYPES = "Payments"
  val GROUP_TAGS          = "Tags"

  val possibleFilter = EventBus[ProgramFilter]()

  def apply() =
    div(
      onMountCallback(_ => ZJS.useBackend(_.program.allFiltersEndpoint(())).emitTo(possibleFilter)),
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(GROUP_TRAINERS, _.trainers),
            renderFilterOptions(GROUP_PAYMENT_TYPES, _.paymentTypes.map(_.toString)),
            renderFilterOptions(GROUP_TAGS, _.tags),
            div(
              cls := "jvm-accordion-search-btn",
              button(
                cls    := "btn btn-primary",
                `type` := "button",
                "Apply Filters"
              )
            )
          )
        )
      )
    )

  def renderFilterOptions(groupName: String, optionsFn: ProgramFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            // stateful Signal + Var
            children <-- possibleFilter.events
              .toSignal(ProgramFilter.empty)
              .map(filter => optionsFn(filter).map(value => renderCheckbox(groupName, value)))
          )
        )
      )
    )

  private def renderCheckbox(groupName: String, value: String) =
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value"
      )
    )
}
