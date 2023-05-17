package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.AvatarProps
import de.matthiasfisch.planningpoker.model.Player
import de.matthiasfisch.planningpoker.model.PlayerStub
import de.matthiasfisch.planningpoker.service.AvatarService
import de.matthiasfisch.planningpoker.service.PlayerService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/v1/players")
class PlayerController(
    private val playerService: PlayerService,
    private val avatarService: AvatarService
) {
    @GetMapping("/me")
    fun getCurrentPlayer(): Player = playerService.getPlayer()

    @GetMapping("/{id}")
    fun getPlayer(@PathVariable("id") id: String): Player = playerService.getPlayer(id)

    @PostMapping
    fun createOrUpdatePlayer(@RequestBody stub: PlayerStub): Player = playerService.getOrUpdatePlayer(stub)

    @GetMapping("/{id}/avatar")
    fun getAvatar(@PathVariable("id") id: String): ResponseEntity<*> {
        val player = playerService.getPlayer(id)
        val avatar = player.avatar
            ?.let { avatarService.getAvatar(it) }
            ?: avatarService.getRandomAvatar(player.name)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/svg+xml"))
            .body(avatar)
    }

    @GetMapping("/avatar/preview")
    fun getAvatarPreview(avatarProps: AvatarProps): ResponseEntity<*> {
        val avatar = avatarService.getAvatar(avatarProps)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("image/svg+xml"))
            .body(avatar)
    }

    @PatchMapping("/me")
    fun updatePlayer(@RequestBody stub: PlayerStub): Player {
        return playerService.updatePlayer(stub)
    }
}