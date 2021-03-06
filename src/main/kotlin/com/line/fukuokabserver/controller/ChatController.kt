package com.line.fukuokabserver.controller

import com.line.fukuokabserver.Auth.Auth
import com.line.fukuokabserver.dto.ChannelDTO
import com.line.fukuokabserver.dto.MessageDTO
import com.line.fukuokabserver.entity.MessageOut
import com.line.fukuokabserver.entity.MessageTest
import com.line.fukuokabserver.service.ChannelDAO
import com.line.fukuokabserver.service.MessageDAO
import com.line.fukuokabserver.service.UserDAO
import org.springframework.http.MediaType
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@Controller
@RestController
class ChatController(private val channelService: ChannelDAO, private val messageService: MessageDAO, private val userService: UserDAO, val auth: Auth) {
    @MessageMapping("/hello")
    @SendTo("/topic/hello")
    fun sendHello(message: MessageTest) : MessageOut {
        return MessageOut(message.from,"Hello World", "")
    }

    @MessageMapping("/chat.{channelId}/test")
    @SendTo("/topic/chat.{channelId}/test")
    fun sendMessage(@DestinationVariable channelId: String, message: MessageTest): MessageOut {
        val time = SimpleDateFormat("HH:mm").format(Date())
        return MessageOut(message.from, message.text, time)
    }

    @MessageMapping("/chat.{channelId}")
    @SendTo("/topic/chat.{channelId}")
    fun sendMessage(@DestinationVariable channelId: String, message: MessageDTO): MessageDTO {
        val time = Timestamp(Date().time)
        message.createdAt = time
        messageService.addMessage(message)
        return message
    }

    @GetMapping(
            value = ["/chat/personal/{userId}/{friendId}"],
            produces = [(MediaType.APPLICATION_JSON_UTF8_VALUE)]
    )
    fun getPersonalChannel(@RequestHeader(value = "Token", required = true) token: String, @PathVariable("userId") userId:Long, @PathVariable("friendId") friendId: Long): ResponseChannelInfo {
        val uid = auth.verifyIdToken(token) ?: throw UnauthorizedException("Invalod Token")
        if (userService.isPersonalChannelExist(userId, friendId)) {
            val channel = channelService.getChannel(userService.getPersonalChannelId(userId, friendId))
            val friend = userService.getUser(friendId)
            val me = userService.getUser(userId)
            val messages = messageService.getChannelMessages(channel.id!!)
            return ResponseChannelInfo(listOf(friend, me), channel, messages)
        }
        else {
            var user = userService.getUser(userId)
            var user2 = userService.getUser(friendId)
            var channel = ChannelDTO(null, "${user.name}と${user2.name}", "PERSONAL")
            channelService.addChannel(channel, listOf(user.Id, user2.Id))
            userService.addPersonalChannel(userId, friendId, channel.id!!)
            return ResponseChannelInfo(listOf(user2, user), channel, emptyList())
        }
    }

    @GetMapping(
            value=["/chat/public"],
            produces = [(MediaType.APPLICATION_JSON_UTF8_VALUE)]
    )
    fun publicChannel(@RequestHeader(value = "Token", required = true) token: String): List<ChannelDTO> {
        val uid = auth.verifyIdToken(token) ?: throw UnauthorizedException("Invalod Token")
        return channelService.getPublicChannel()
    }

    @GetMapping(
            value = ["/chat/messages/{channelId}"],
            produces = [(MediaType.APPLICATION_JSON_UTF8_VALUE)]
    )
    fun getMessages(@RequestHeader(value = "Token", required = true) token: String, @PathVariable("channelId") channelId: Long): List<MessageDTO> {
        val uid = auth.verifyIdToken(token) ?: throw UnauthorizedException("Invalod Token")
        return messageService.getChannelMessages(channelId)
    }

    @PostMapping(
            value = ["/chat/group/new"],
            produces = [(MediaType.APPLICATION_JSON_UTF8_VALUE)]
    )
    fun newGroupChannel(@RequestHeader(value = "Token", required = true) token: String, @RequestBody request: PostNewGroup): ResponseChannelInfo {
        val uid = auth.verifyIdToken(token) ?: throw UnauthorizedException("Invalod Token")
        val users = userService.getUsers(request.userIds)
        var defaultName = ""
        for (i in 0..users.size-1) {
            defaultName += users[i].name
            if (i != users.size-1) defaultName += ", "
        }
        var channel = ChannelDTO(null, defaultName, "GROUP")
        channelService.addChannel(channel, request.userIds)
        return ResponseChannelInfo(users, channel, emptyList())
    }

    @GetMapping(
            value = ["chat/{channelId}/info"],
            produces = [(MediaType.APPLICATION_JSON_UTF8_VALUE)]
    )
    fun getChannelInfo(@RequestHeader(value = "Token", required = true) token: String, @PathVariable("channelId") channelId: Long): ResponseChannelInfo {
        val uid = auth.verifyIdToken(token) ?: throw UnauthorizedException("Invalod Token")
        val channel = channelService.getChannel(channelId)
        val users = channelService.getChannelAttendees(channelId)
        val messages = messageService.getChannelMessages(channelId)
        return ResponseChannelInfo(users, channel, messages)
    }


    @GetMapping(
            value = ["chat/{userId}/channels"],
            produces = [(MediaType.APPLICATION_JSON_UTF8_VALUE)]
    )
    fun getChannelsByUser(@RequestHeader(value = "Token", required = true) token: String, @PathVariable("userId") userId: Long): List<ChannelDTO> {
        val uid = auth.verifyIdToken(token) ?: throw UnauthorizedException("Invalod Token")
        return channelService.getChannelList(userId)
    }

    @PostMapping(
            value = ["chat/{channelId}/update/name"]
    )
    fun updateChannelName(@PathVariable("channelId") channelId: Long, @RequestBody request: PostUpdateChannelName) {
        var channel = channelService.getChannel(channelId)
        channel.name = request.name
        channelService.updateChannel(channel)
    }
}