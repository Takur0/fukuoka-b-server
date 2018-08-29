package com.line.fukuokabserver.service

import com.line.fukuokabserver.dto.UserDTO
import com.line.fukuokabserver.mapper.UserMapper
import org.springframework.stereotype.Service

@Service
class UserDAO (private val userMapper: UserMapper): IUserDAO {
    override fun getUsers(ids: List<Long>): List<UserDTO> {
        return ids.map { getUser(it) }.toList()
    }

    override fun getUserByMail(mail: String): UserDTO {
        return userMapper.findByMail(mail)
    }

    override fun getUser(id: Long): UserDTO {
        return userMapper.findById(id)
    }

    override fun getFriendList(userId: Long): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addUser(user: UserDTO) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addFriend(id1: Long, id2: Long) {
        userMapper.addFriend(id1, id2)
        userMapper.addFriend(id2, id1)
    }

    override fun updateUser(userId: Long, user: UserDTO) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}