package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.service.ThemeService
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.compress.utils.IOUtils
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("api/v1/games")
class ThemeController(
    private val themeService: ThemeService
) {

    @PostMapping("/{gameId}/theme/card-icon")
    fun setCardIcon(@PathVariable("gameId") gameId: String, @RequestParam("file") file: MultipartFile) {
        themeService.setCardIcon(gameId, file.inputStream)
    }

    @GetMapping("/{gameId}/theme/card-icon")
    fun getCardIcon(@PathVariable("gameId") gameId: String, response: HttpServletResponse) {
        response.outputStream.use {  output ->
            themeService.getCardIcon(gameId).use {  input ->
                IOUtils.copy(input, output)
            }
        }
        response.contentType = "image/png"
        response.status = 200
    }
}