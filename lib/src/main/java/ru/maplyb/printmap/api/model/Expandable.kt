package ru.maplyb.printmap.api.model

interface Expandable {
    val selected: Boolean
    val name: String
    val header: String?
}