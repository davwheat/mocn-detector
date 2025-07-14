package dev.davwheat.mocndetector

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = ZonedDateTime::class)
object ZonedDateTimeSerializer : KSerializer<ZonedDateTime> {
    private val formatter = DateTimeFormatter.ISO_ZONED_DATE_TIME

    override fun serialize(encoder: Encoder, value: ZonedDateTime) =
        encoder.encodeString(value.format(formatter))

    override fun deserialize(decoder: Decoder): ZonedDateTime =
        ZonedDateTime.parse(decoder.decodeString(), formatter)
}
