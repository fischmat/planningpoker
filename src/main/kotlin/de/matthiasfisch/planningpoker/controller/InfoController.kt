package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.ApplicationInfo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/info")
class InfoController(
    private val applicationInfo: ApplicationInfo
) {
    @GetMapping
    fun getInfo() = applicationInfo
}