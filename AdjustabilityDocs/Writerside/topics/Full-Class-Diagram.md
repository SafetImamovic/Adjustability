# Full Class Diagram

<chapter collapsible="true" title="Access Modifier Notation">
<p>Characters that define the visibility:</p>
<table>
<tr><th>Character</th><th>Icon for field</th><th>Icon for method</th><th>Visibility</th></tr>
<tr><td><code>-</code></td><td>&#9723;</td><td>&#9724;</td><td><code>private</code></td></tr>
<tr><td><code>#</code></td><td>&#9671;</td><td>&#9670;</td><td><code>protected</code></td></tr>
<tr><td><code>~</code></td><td>&#9651;</td><td>&#9650;</td><td><code>package private</code></td></tr>
<tr><td><code>+</code></td><td>&#9675;</td><td>&#9679;</td><td><code>public</code></td></tr>
</table>
</chapter>

```plantuml
@startuml
top to bottom direction
skinparam linetype ortho

class Adjustability {
  + Adjustability(): 
  + MODID: String
  - LOGGER: Logger
  - commonSetup(FMLCommonSetupEvent): void
}
package enchantments_logic
{
class AnvilHandler
{
  + AnvilHandler(): 
  + onAnvilUpdate(AnvilUpdateEvent): void
  + getEnchantment(ItemStack): Enchantment?
  - areMutuallyExclusive(ItemStack, ItemStack): boolean
  + getEnchantmentsList(ItemStack): List<Enchantment>
  - hasEnchantments(ItemStack): boolean
  }
}

package gui_elements
{
class Button {
  + Button(int, int, int, int, int, String): 
  + Button(): 
  - height: int
  - bgColor: int
  - text: String
  - width: int
  - y: int
  - BORDER_COLOR_BOTTOM: int
  - x: int
  - BORDER_COLOR_TOP: int
  + hover(double, double): void
  + click(double, double): void
  + setProperties(int, int, int, int, int, String): void
  + setPosition(int, int): void
  + render(PoseStack): void
}
}

class Config {
  + Config(): 
  - BUILDER: Builder
  ~ SPEC: ForgeConfigSpec
}
class CustomTooltip {
  + CustomTooltip(): 
  - arraySet: boolean
  ~ MOUSE_OFFSET: int
  ~ PADDING: int
  ~ BORDER_COLOR_TOP: int
  - Buttons: List<Button>
  ~ BACKGROUND_COLOR: int
  ~ BORDER_COLOR_BOTTOM: int
  ~ BLENDED_COLOR: int
  - clickActive: boolean
  ~ x: int
  ~ y: int
  ~ rendering: boolean
  + drawBaseRectangleWithBorders(Render, Vector2ic): void
  + positionTooltip(int, int, Vector2i, int, int): void
  + drawDecorationsAroundTooltip(Render, Vector2ic): void
  + click(MouseButtonPressed): void
  + onClickTickWise(MouseButton): void
  + letGoOfKey(KeyReleased): void
  + blockScreenInputMouseClickRelease(Pre): void
  + blockScreenInputMouseDragged(Pre): void
  + drawBoxBorders(Render): void
  - shouldRender(int, int): boolean
  + positionTooltip(int, int, int, int, int, int): Vector2ic
  + blockScreenInputMouseScroll(Pre): void
  + cancelDefaultTooltip(RenderTooltipEvent): void
  + addBottomInformationToTheDefaultTooltipForOpeningTheCustomTooltipGui(ItemTooltipEvent): void
  + blockScreenInputMouseClick(Pre): void
  + isCursorWithinGUIBoundaries(int, int, int, int): boolean
}
package render_core
{
class RenderCore {
  + RenderCore(): 
  + renderText(String, float, float, int, int, boolean, GuiGraphics): void
  + drawGradientRectangle(Matrix4f, int, int, int, int, int, int, int): void
  + drawBoxBorders(PoseStack, int, int, int, int, int, int, boolean, int): void
  + drawRectangle(PoseStack, int, int, int, int, int, int, int): void
  + drawGradientRectangle(Matrix4f, BufferBuilder, int, int, int, int, int, int, int): void
  + isCursorWithinGUIBoundaries(int, int, int, int, int, int): boolean
}
}


Adjustability  --->  AnvilHandler  : <<create>>
Adjustability  --->  CustomTooltip : <<create>>
AnvilHandler   --->  Adjustability 
Button         --->  RenderCore    
Config         --->  Adjustability 
CustomTooltip  --->  Adjustability 
CustomTooltip  --->  Button        : <<create>>
CustomTooltip "1" *---> "Buttons\n*" Button        
CustomTooltip  --->  RenderCore : <<uses>>
Button  --->  RenderCore : <<uses>>
@enduml
```