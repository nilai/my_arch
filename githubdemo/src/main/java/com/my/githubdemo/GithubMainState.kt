package com.my.githubdemo

import com.my.dataflow.Async
import com.my.dataflow.MvRxState
import com.my.dataflow.Uninitialized
import com.my.githubdemo.github.GithubRepository
import com.my.githubdemo.github.GithubSearchResults

data class GithubMainState(val page: Int = 0,
                      val items: List<GithubRepository> = emptyList(),
                      val request: Async<GithubSearchResults> = Uninitialized) : MvRxState