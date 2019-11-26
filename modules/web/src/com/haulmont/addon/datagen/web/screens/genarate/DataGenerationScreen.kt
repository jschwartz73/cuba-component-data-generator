package com.haulmont.addon.datagen.web.screens.genarate

import com.haulmont.addon.datagen.generation.DataGenerationSettings
import com.haulmont.addon.datagen.entity.*
import com.haulmont.addon.datagen.entity.number.NumberPropGenSettings
import com.haulmont.addon.datagen.service.DataGenerationService
import com.haulmont.addon.datagen.web.screens.props.booleanpropertygenerationsettings.BooleanPropertyGenerationSettingsFragment
import com.haulmont.addon.datagen.web.screens.props.numberpropgensettings.NumberPropGenSettingsFragment
import com.haulmont.addon.datagen.web.screens.props.stringpropertygenerationsettings.StringPropertyGenerationSettingsFragment
import com.haulmont.chile.core.model.MetaClass
import com.haulmont.chile.core.model.MetaProperty
import com.haulmont.cuba.core.entity.Entity
import com.haulmont.cuba.core.global.Metadata
import com.haulmont.cuba.gui.Fragments
import com.haulmont.cuba.gui.UiComponents
import com.haulmont.cuba.gui.components.Button
import com.haulmont.cuba.gui.components.Component
import com.haulmont.cuba.gui.components.LookupField
import com.haulmont.cuba.gui.components.ScrollBoxLayout
import com.haulmont.cuba.gui.components.data.options.ListOptions
import com.haulmont.cuba.gui.model.CollectionPropertyContainer
import com.haulmont.cuba.gui.model.DataComponents
import com.haulmont.cuba.gui.model.InstanceContainer
import com.haulmont.cuba.gui.screen.*
import com.haulmont.cuba.web.gui.components.WebGroupBox
import javax.inject.Inject

@Suppress("IncorrectCreateEntity")
@UiController("datagen_Generation")
@UiDescriptor("data-generation.xml")
class DataGenerationScreen : Screen() {

    // Services
    @Inject
    private lateinit var metadata: Metadata
    @Inject
    private lateinit var uiComponents: UiComponents
    @Inject
    private lateinit var dataComponents: DataComponents
    @Inject
    private lateinit var fragments: Fragments
    @Inject
    private lateinit var dataGenerationService: DataGenerationService

    // Data
    @Inject
    private lateinit var generationCommandDc: InstanceContainer<DataGenerationCommand<Entity<*>>>
    @Inject
    private lateinit var propertiesDc: CollectionPropertyContainer<PropertyGenerationSettings>

    // UI
    @Inject
    private lateinit var entityLookup: LookupField<MetaClass>
    @Inject
    private lateinit var propertiesConfigBox: ScrollBoxLayout

    @Subscribe
    private fun onInit(event: InitEvent) {
        generationCommandDc.setItem(DataGenerationCommand())
        updateEntitySelectLookup()
    }

    private fun updateEntitySelectLookup() {
        val metaClasses = metadata.tools.allPersistentMetaClasses
                .sortedBy { it.name }
        entityLookup.options = ListOptions(metaClasses)
        entityLookup.addValueChangeListener { selectMetaClass(it.value) }
    }

    private fun selectMetaClass(metaClass: MetaClass?) {
        propertiesConfigBox.removeAll()
        if (metaClass == null) {
            generationCommandDc.item.entityGenerationSettings = null
            return
        }
        val entityGenerationSettings = EntityGenerationSettings(metaClass.getJavaClass<Entity<*>>())
        generationCommandDc.item.entityGenerationSettings = entityGenerationSettings

        metaClass.properties
                .filter { shouldBeGenerated(it) }
                .forEach {
                    val propSettings = DataGenerationSettings.createSettings(it) ?: return
                    entityGenerationSettings.properties.add(propSettings)
                    propertiesConfigBox.add(createPropertyGenerationUI(it, propSettings))
                }
    }

    @Subscribe("generateBtn")
    private fun onGenerateBtnClick(event: Button.ClickEvent) {
        dataGenerationService.generateEntities(this.generationCommandDc.item)
    }


    private fun createPropertyGenerationUI(
            prop: MetaProperty,
            settings: PropertyGenerationSettings
    ): Component {
        val propDC = dataComponents.createInstanceContainer(
                PropertyGenerationSettings::class.java,
                propertiesDc,
                prop.name.toString()
        )
        propDC.setItem(settings)
        screenData.registerContainer("${prop.name}Dc", propDC) // todo unregister

        val webGroupBox = uiComponents.create<WebGroupBox>(WebGroupBox::class.java)
        webGroupBox.caption = prop.name
        val createPropFragment = createPropFragment(settings)
        webGroupBox.add(createPropFragment.fragment)
        return webGroupBox
    }

    fun createPropFragment(propSettings: PropertyGenerationSettings): ScreenFragment {
        return when (propSettings) {
            is BooleanPropertyGenerationSettings -> {
                fragments.create(this, BooleanPropertyGenerationSettingsFragment::class.java)
                        .setItem(propSettings)
            }
            is StringPropertyGenerationSettings ->
                fragments.create(this, StringPropertyGenerationSettingsFragment::class.java)
                        .setItem(propSettings)
            is NumberPropGenSettings ->
                fragments.create(this, NumberPropGenSettingsFragment::class.java)
                        .setItem(propSettings)
            else -> throw IllegalStateException("Unsupported Property")
        }
    }

    private fun shouldBeGenerated(it: MetaProperty) =
            DataGenerationSettings.isGeneratorAvailable(it)
                    && !metadata.tools.isSystem(it)
                    && metadata.tools.isPersistent(it)


}

