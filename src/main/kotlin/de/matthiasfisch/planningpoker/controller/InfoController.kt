package de.matthiasfisch.planningpoker.controller

import de.matthiasfisch.planningpoker.model.ApplicationInfo
import de.matthiasfisch.planningpoker.model.SocketIOInfo
import de.matthiasfisch.planningpoker.service.EventService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URL

@RestController
@RequestMapping("/api/v1/info")
class InfoController(
    private val eventService: EventService,
    @Value("\${server.url}") private val serverUrl: String,
    @Value("\${websockets.public-port}") private val socketIOPort: Int,
    @Value("\${websockets.protocol}") private val socketIOProto: String
) {

    @GetMapping
    fun getInfo() =
        ApplicationInfo(
            socketIO = SocketIOInfo(
                host = URL(serverUrl).host,
                port = socketIOPort,
                scheme = socketIOProto
            )
        )
}