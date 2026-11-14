package com.campusquest.ui.creator.validation

object GameValidator {

    /** Returns an error message, or null if the title is OK. */
    fun validateTitle(input: String): String? {
        val title = input.trim()
        if (title.isEmpty()) return "Title is required"
        if (title.length > CreatorLimits.TITLE_MAX_LENGTH)
            return "Title must be at most ${CreatorLimits.TITLE_MAX_LENGTH} characters"
        return null
    }

    /** Drafts may leave the description empty; publishing may not. */
    fun validateDescription(input: String, forPublish: Boolean): String? {
        val description = input.trim()
        if (forPublish && description.isEmpty()) return "Description is required to publish"
        if (description.length > CreatorLimits.DESCRIPTION_MAX_LENGTH)
            return "Description must be at most ${CreatorLimits.DESCRIPTION_MAX_LENGTH} characters"
        return null
    }
}