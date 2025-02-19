package com.kylecorry.trail_sense.navigation.beacons.infrastructure.persistence

import android.content.Context
import androidx.lifecycle.LiveData
import com.kylecorry.trail_sense.navigation.beacons.domain.BeaconOwner
import com.kylecorry.trail_sense.shared.database.AppDatabase

class BeaconRepo private constructor(context: Context) : IBeaconRepo {

    private val beaconDao = AppDatabase.getInstance(context).beaconDao()
    private val beaconGroupDao = AppDatabase.getInstance(context).beaconGroupDao()

    override fun getBeacons(): LiveData<List<BeaconEntity>> = beaconDao.getAll()

    override suspend fun getBeaconsSync(): List<BeaconEntity> = beaconDao.getAllSuspend()

    override suspend fun searchBeacons(text: String): List<BeaconEntity> = beaconDao.search(text)

    override suspend fun searchBeaconsInGroup(text: String, groupId: Long?): List<BeaconEntity> =
        beaconDao.searchInGroup(text, groupId)

    override suspend fun getBeaconsInGroup(groupId: Long?): List<BeaconEntity> =
        beaconDao.getAllInGroup(groupId)

    override suspend fun getBeacon(id: Long): BeaconEntity? = beaconDao.get(id)

    override suspend fun getTemporaryBeacon(owner: BeaconOwner): BeaconEntity? =
        beaconDao.getTemporaryBeacon(owner.id)

    override suspend fun deleteBeacon(beacon: BeaconEntity) = beaconDao.delete(beacon)

    override suspend fun addBeacon(beacon: BeaconEntity): Long {
        return if (beacon.id != 0L) {
            beaconDao.update(beacon)
            beacon.id
        } else {
            beaconDao.insert(beacon)
        }
    }

    override suspend fun addBeaconGroup(group: BeaconGroupEntity): Long {
        return if (group.id != 0L) {
            beaconGroupDao.update(group)
            group.id
        } else {
            beaconGroupDao.insert(group)
        }
    }

    override suspend fun deleteBeaconGroup(group: BeaconGroupEntity) {
        // Delete beacons
        beaconDao.deleteInGroup(group.id)

        // Delete groups
        val groups = getGroupsWithParent(group.id)
        for (subGroup in groups){
            deleteBeaconGroup(subGroup)
        }

        // Delete self
        beaconGroupDao.delete(group)
    }

    override suspend fun getGroupsWithParent(parent: Long?): List<BeaconGroupEntity> =
        beaconGroupDao.getAllWithParent(parent)

    override suspend fun getGroup(id: Long): BeaconGroupEntity? = beaconGroupDao.get(id)


    companion object {
        private var instance: BeaconRepo? = null

        @Synchronized
        fun getInstance(context: Context): BeaconRepo {
            if (instance == null) {
                instance = BeaconRepo(context.applicationContext)
            }
            return instance!!
        }
    }

}