package com.example.itemmanagementandroid.domain.model

class DuplicateItemNameException : IllegalArgumentException(
    "Item name already exists."
)
