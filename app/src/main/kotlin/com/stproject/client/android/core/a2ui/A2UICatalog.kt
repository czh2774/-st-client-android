package com.stproject.client.android.core.a2ui

object A2UICatalog {
    const val CATALOG_ID = "https://stproject.ai/a2ui/v0.8/catalogs/st-mobile-safe.json"

    object Components {
        const val TEXT = "Text"
        const val IMAGE = "Image"
        const val BUTTON = "Button"
        const val CHOICE_BUTTONS = "ChoiceButtons"
        const val FORM = "Form"
        const val SHEET = "Sheet"
        const val PURCHASE_CTA = "PurchaseCTA"
        const val OPEN_SETTINGS = "OpenSettings"
        const val COLUMN = "Column"
        const val ROW = "Row"
        const val GROUP = "Group"
    }

    object Actions {
        const val SEND_MESSAGE = "sendMessage"
        const val CANCEL = "cancel"
        const val CONTINUE = "continue"
        const val REGENERATE = "regenerate"
        const val SET_VARIABLE = "setVariable"
        const val NAVIGATE = "navigate"
        const val PURCHASE = "purchase"
        const val DELETE_MESSAGE = "deleteMessage"
    }

    val COMPONENTS: List<String> =
        listOf(
            Components.TEXT,
            Components.IMAGE,
            Components.BUTTON,
            Components.CHOICE_BUTTONS,
            Components.FORM,
            Components.SHEET,
            Components.PURCHASE_CTA,
            Components.OPEN_SETTINGS,
            Components.COLUMN,
            Components.ROW,
            Components.GROUP,
        )

    val ACTIONS: List<String> =
        listOf(
            Actions.SEND_MESSAGE,
            Actions.CANCEL,
            Actions.CONTINUE,
            Actions.REGENERATE,
            Actions.SET_VARIABLE,
            Actions.NAVIGATE,
            Actions.PURCHASE,
            Actions.DELETE_MESSAGE,
        )

    private val componentSet = COMPONENTS.toSet()
    private val actionSet = ACTIONS.toSet()

    fun supportsComponent(type: String): Boolean = componentSet.contains(type)

    fun supportsAction(name: String): Boolean = actionSet.contains(name)
}
