package io.wispershadow.tech.common.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

//usage @JsonDeserialize(using = KeepAsJsonDeserializer::class)
//can deserialize nested json string, to serialize nested json, use @JsonRawValue
class KeepAsJsonDeserializer: JsonDeserializer<String>() {
    override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
        val tree: TreeNode = jp.codec.readTree(jp);
        return tree.toString();
    }
}