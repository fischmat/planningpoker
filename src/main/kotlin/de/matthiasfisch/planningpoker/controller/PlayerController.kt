package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.PlayerStub
import de.matthiasfisch.planningpoker.service.PlayerService
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/players")
class PlayerController(
    private val playerService: PlayerService
) {
    @GetMapping("/{id}")
    fun getPlayer(@PathVariable("id") id: String): Player = playerService.getPlayer(id)

    @PostMapping
    fun createOrUpdatePlayer(@RequestBody stub: PlayerStub): Player = playerService.getOrCreatePlayer(stub.name)
}