// EliteFourManager.kt

package com.cobblegyms.system.e4

class EliteFourManager {
    private val members = mutableListOf<E4Member>()

    fun addMember(member: E4Member) {
        members.add(member)
    }

    fun removeMember(member: E4Member) {
        members.remove(member)
    }

    fun getMembers(): List<E4Member> {
        return members
    }

    fun findMember(name: String): E4Member? {
        return members.find { it.name == name }
    }
}

data class E4Member(val name: String, val type: String, val level: Int)