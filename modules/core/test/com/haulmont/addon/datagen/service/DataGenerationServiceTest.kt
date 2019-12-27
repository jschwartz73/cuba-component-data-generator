package com.haulmont.addon.datagen.service

import com.haulmont.addon.datagen.DatagenTestContainer
import com.haulmont.addon.datagen.entity.*
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.Metadata
import com.haulmont.cuba.security.entity.Permission
import org.junit.After
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test

@Suppress("IncorrectCreateEntity")
class DataGenerationServiceTest {

    companion object {
        @ClassRule @JvmField
        var cont: DatagenTestContainer = DatagenTestContainer.Common.INSTANCE
    }

    lateinit var dgs: DataGenerationService
    lateinit var metadata: Metadata

    @Before
    @Throws(Exception::class)
    fun setUp() {
        dgs = AppBeans.get(DataGenerationService::class.java)
        metadata = AppBeans.get(Metadata::class.java)
    }

    @Test
    fun testGeneratedEntityClass() {
        val settings = EntityGenerationSettings<TestEntityJava>()
        settings.entityClass = TestEntityJava::class.java
        val generated = dgs.generateEntity(settings)
        assert(TestEntityJava::class.java.isAssignableFrom(generated::class.java))
    }

    @Test
    fun testAmount() {
        val command = DataGenerationCommand<TestEntityJava>()
        command.amount = 3
        command.entityGenerationSettings = EntityGenerationSettings()
        command.entityGenerationSettings.entityClass = TestEntityJava::class.java
        command.type = DataGenerationType.JSON
        val res = dgs.generateEntities(command)
        assert(res.generated.size == 3)
        assert(res.committed.size == 0)
    }

    @Test
    fun testCommitSeparately() {
        val command = DataGenerationCommand<Permission>()
        command.amount = 3
        command.entityGenerationSettings = EntityGenerationSettings()
        command.entityGenerationSettings.entityClass = Permission::class.java
        command.type = DataGenerationType.COMMIT_SEPARATELY
        val res = dgs.generateEntities(command)
        assert(res.exceptions.size == 3) // todo
        assert(res.generated.size == 3)
    }

//    @Test
//    fun testEntityWithAllSupportedPropertiesGenerated() {
//        val settings = EntityGenerationSettings(TestEntity::class.java)
//        val metaClass = metadata.getClass(TestEntity::class.java)
//        val generated = dgs.generateEntity(settings)
//        TestEntity::class.declaredMemberProperties.forEach {kProp ->
//
//            val metaProp = metaClass!!.getProperty(kProp.name) ?: return
//
//            if (!PropertyGeneration.isGeneratorAvailable(metaProp)) return
//
//            val generatedProp = kProp.get(generated)
//            assert(generatedProp != null)
//        }
//    }

    @After
    @Throws(Exception::class)
    fun tearDown() {
    }
}
