package li.cil.oc.server.driver.item

import li.cil.oc.api
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.tileentity.Rotatable
import li.cil.oc.common.Slot
import li.cil.oc.server.component
import net.minecraft.item.ItemStack

object UpgradePiston extends Item {
  override def worksWith(stack: ItemStack) =
    isOneOf(stack, api.Items.get("pistonUpgrade"))

  override def worksWith(stack: ItemStack, host: Class[_ <: EnvironmentHost]) =
    super.worksWith(stack, host) && isRotatable(host)

  override def createEnvironment(stack: ItemStack, host: EnvironmentHost) = host match {
    case rotatable: Rotatable with EnvironmentHost => new component.UpgradePiston(rotatable)
    case _ => null
  }

  override def slot(stack: ItemStack) = Slot.Upgrade
}
