package com.my.githubdemo.github

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubSearchResults(
        val items: List<GithubRepository>
)
