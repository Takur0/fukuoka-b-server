package com.line.fukuokabserver.dto

import java.sql.Timestamp

data class MessageDTO (
        var id: Long,
        var senderId: Long,
        var roomId: Long,
        var text: String,
        var sendAt: Timestamp
)