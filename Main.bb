;This is the Source Code from SCP:CB Version 1.3.0. This version was created by the "Third Subdivision Team".
;Original credit goes to Regalis and all the other contributers to SCP:CB.

Local InitErrorStr$ = ""
If FileSize("FastExt.dll")=0 Then InitErrorStr=InitErrorStr+ "FastExt.dll"+Chr(13)+Chr(10)
If FileSize("FastText.dll")=0 Then InitErrorStr=InitErrorStr+ "FastText.dll"+Chr(13)+Chr(10)
If FileSize("winfix.dll")=0 Then InitErrorStr=InitErrorStr+ "winfix.dll"+Chr(13)+Chr(10)

If Len(InitErrorStr)>0 Then
	RuntimeError "The following DLLs were not found in the game directory:"+Chr(13)+Chr(10)+Chr(13)+Chr(10)+InitErrorStr
EndIf

Include "FastExt.bb"
Include "FastText_Unicode.bb"
Include "StrictLoads.bb"
Include "fullscreen_window_fix.bb"

CompatData%(12, 0) ;hopefully this fixes performance issues on Windows 8
Global OptionFile$ = "options.ini"

Global Font1%, Font2%, Font3%, Font4%

Global VersionNumber$ = "1.3.0"

AppTitle "SCP - Containment Breach Launcher"

Global MenuWhite%, MenuBlack%
Global ButtonSFX%

BumpPower 0.03

Dim ArrowIMG(4)

;[Block]

Global LauncherWidth%= Min(GetINIInt(OptionFile, "launcher", "launcher width"), 1024)
Global LauncherHeight% = Min(GetINIInt(OptionFile, "launcher", "launcher height"), 768)
Global LauncherEnabled% = GetINIInt(OptionFile, "launcher", "launcher enabled")
Global LauncherIMG%

Global GraphicWidth% = GetINIInt(OptionFile, "options", "width")
Global GraphicHeight% = GetINIInt(OptionFile, "options", "height")
Global Depth% = 0, Fullscreen% = GetINIInt(OptionFile, "options", "fullscreen")

Global SelectedGFXMode%
Global SelectedGFXDriver% = Max(GetINIInt(OptionFile, "options", "gfx driver"), 1)

Global fresize_image%, fresize_texture
Global fresize_cam%

Global ShowFPS = GetINIInt(OptionFile, "options", "show FPS"), WireframeState

Global TotalGFXModes% = CountGfxModes3D(), GFXModes%
Dim GfxModeWidths%(TotalGFXModes), GfxModeHeights%(TotalGFXModes)

Global FakeFullScreen% = GetINIInt(OptionFile, "options", "fakefullscreen")

If LauncherEnabled Then 
	UpdateLauncher()
	
	;New "fake fullscreen" - ENDSHN
	If FakeFullScreen
		DebugLog "Using Faked Fullscreen"
		Graphics3DExt G_viewport_width, G_viewport_height, 0, 2
		
		; -- Change the window style to 'WS_POPUP' and then set the window position to force the style to update.
		api_SetWindowLong( G_app_handle, C_GWL_STYLE, C_WS_POPUP )
		api_SetWindowPos( G_app_handle, C_HWND_TOP, G_viewport_x, G_viewport_y, G_viewport_width, G_viewport_height, C_SWP_SHOWWINDOW )
		
		GraphicWidth = G_viewport_width
		GraphicHeight = G_viewport_height
		
		Fullscreen = False
	Else
		If Fullscreen Then
			Graphics3DExt(GraphicWidth, GraphicHeight, Depth, 1)
		Else
			Graphics3DExt(GraphicWidth, GraphicHeight, Depth, 2)
		End If
	EndIf
	
Else
	For i% = 1 To TotalGFXModes
		Local samefound% = False
		For  n% = 0 To TotalGFXModes - 1
			If GfxModeWidths(n) = GfxModeWidth(i) And GfxModeHeights(n) = GfxModeHeight(i) Then samefound = True : Exit
		Next
		If samefound = False Then
			If GraphicWidth = GfxModeWidth(i) And GraphicHeight = GfxModeHeight(i) Then SelectedGFXMode = GFXModes
			GfxModeWidths(GFXModes) = GfxModeWidth(i)
			GfxModeHeights(GFXModes) = GfxModeHeight(i)
			GFXModes=GFXModes+1
		End If
	Next
	
	GraphicWidth = GfxModeWidths(SelectedGFXMode)
	GraphicHeight = GfxModeHeights(SelectedGFXMode)
	
	;New "fake fullscreen" - ENDSHN
	If FakeFullScreen
		DebugLog "Using Faked Fullscreen"
		Graphics3DExt G_viewport_width, G_viewport_height, 0, 2
		
		; -- Change the window style to 'WS_POPUP' and then set the window position to force the style to update.
		api_SetWindowLong( G_app_handle, C_GWL_STYLE, C_WS_POPUP )
		api_SetWindowPos( G_app_handle, C_HWND_TOP, G_viewport_x, G_viewport_y, G_viewport_width, G_viewport_height, C_SWP_SHOWWINDOW )
		
		GraphicWidth = G_viewport_width
		GraphicHeight = G_viewport_height
		
		Fullscreen = False
	Else
		If Fullscreen Then
			Graphics3DExt(GraphicWidth, GraphicHeight, Depth, 1)
		Else
			Graphics3DExt(GraphicWidth, GraphicHeight, Depth, 2)
		End If
	EndIf
	
EndIf

Global MenuScale# = (GraphicHeight / 1024.0)

SetBuffer BackBuffer()

Global CurTime%, PrevTime%, LoopDelay%, FPSfactor#, FPSfactor2#
Local CheckFPS%, ElapsedLoops%, FPS%, ElapsedTime#

Local Framelimit% = GetINIInt(OptionFile, "options", "framelimit")
Local Vsync% = GetINIInt(OptionFile, "options", "vsync")

Global ScreenGamma# = GetINIFloat(OptionFile, "options", "screengamma")
If Fullscreen Then UpdateScreenGamma()

Const HIT_MAP% = 1, HIT_PLAYER% = 2, HIT_ITEM% = 3, HIT_APACHE% = 4, HIT_178% = 5
SeedRnd MilliSecs()

;[End block]

Global GameSaved%

AppTitle "SCP - Containment Breach v"+VersionNumber

;---------------------------------------------------------------------------------------------------------------------

;[Block]

Global CursorIMG% = LoadImage_Strict("GFX\cursor.png")

Global SelectedLoadingScreen.LoadingScreens, LoadingScreenAmount%, LoadingScreenText%
Global LoadingBack% = LoadImage_Strict("Loadingscreens\loadingback.jpg")
InitLoadingScreens("Loadingscreens\loadingscreens.ini")

Font1% = LoadFont_Strict("GFX\cour.ttf", Int(18 * (GraphicHeight / 1024.0)), 0,0,0,0, FT_DEFAULT)
Font2% = LoadFont_Strict("GFX\courbd.ttf", Int(58 * (GraphicHeight / 1024.0)), 0,0,0,0, FT_DEFAULT)
Font3% = LoadFont_Strict("GFX\DS-DIGI.ttf", Int(22 * (GraphicHeight / 1024.0)), 0,0,0,0, FT_DEFAULT)
Font4% = LoadFont_Strict("GFX\DS-DIGI.ttf", Int(60 * (GraphicHeight / 1024.0)), 0,0,0,0, FT_DEFAULT)
SetFont Font2

Global BlinkMeterIMG% = LoadImage_Strict("GFX\blinkmeter.jpg")

DrawLoading(0, True)

; - -Viewport.
Global viewport_center_x% = GraphicWidth / 2, viewport_center_y% = GraphicHeight / 2

; -- Mouselook.
Global mouselook_x_inc# = 0.3 ; This sets both the sensitivity and direction (+/-) of the mouse on the X axis.
Global mouselook_y_inc# = 0.3 ; This sets both the sensitivity and direction (+/-) of the mouse on the Y axis.
; Used to limit the mouse movement to within a certain number of pixels (250 is used here) from the center of the screen. This produces smoother mouse movement than continuously moving the mouse back to the center each loop.
Global mouse_left_limit% = 250, mouse_right_limit% = GraphicsWidth () - 250
Global mouse_top_limit% = 150, mouse_bottom_limit% = GraphicsHeight () - 150 ; As above.
Global mouse_x_speed_1#, mouse_y_speed_1#

Global KEY_RIGHT=GetINIInt(OptionFile, "options", "Right key"), KEY_LEFT=GetINIInt(OptionFile, "options", "Left key")
Global KEY_UP=GetINIInt(OptionFile, "options", "Up key"), KEY_DOWN=GetINIInt(OptionFile, "options", "Down key")
Global KEY_BLINK=GetINIInt(OptionFile, "options", "Blink key"), KEY_SPRINT=GetINIInt(OptionFile, "options", "Sprint key")
Global KEY_INV=GetINIInt(OptionFile, "options", "Inventory key"), KEY_CROUCH=GetINIInt(OptionFile, "options", "Crouch key")

Const INFINITY# = (999.0) ^ (99999.0), NAN# = (-1.0) ^ (0.5)

Global Mesh_MinX#, Mesh_MinY#, Mesh_MinZ#
Global Mesh_MaxX#, Mesh_MaxY#, Mesh_MaxZ#
Global Mesh_MagX#, Mesh_MagY#, Mesh_MagZ#

;player stats -------------------------------------------------------------------------------------------------------
Global KillTimer#, KillAnim%, FallTimer#, DeathTimer#
Global Sanity#, ForceMove#, ForceAngle#

Global Playable% = True

Const BLINKFREQ% = 70 * 8
Global BlinkTimer#, EyeIrritation#, EyeStuck#, BlinkEffect# = 1.0, BlinkEffectTimer#

Global Stamina#, StaminaEffect#=1.0, StaminaEffectTimer#

Global SCP1025state#[6]

Global HeartBeatRate#, HeartBeatTimer#, HeartBeatVolume#

Global WearingGasMask%, WearingHazmat%, WearingVest%, Wearing714%, WearingNightVision%, Wearing178%
Global NVTimer#

Global SuperMan%, SuperManTimer#

Global Injuries#, Bloodloss#, Infect#

Global RefinedItems%

Include "Achievements.bb"

;player coordinates, angle, speed, movement etc ---------------------------------------------------------------------
Global DropSpeed#, HeadDropSpeed#, CurrSpeed#
Global user_camera_pitch#, side#
Global Crouch%, CrouchState#

Global PlayerZone%, PlayerRoom.Rooms

Global GrabbedEntity%

Global InvertMouse% = GetINIInt(OptionFile, "options", "invert mouse y")
Global MouseHit1%, MouseDown1%, MouseHit2%, DoubleClick%, LastMouseHit1%, MouseUp1%

Global GodMode%, NoClip%, NoClipSpeed# = 2.0

Global CoffinDistance#

Global PlayerSoundVolume#

;camera/lighting effects (blur, camera shake, etc)-------------------------------------------------------------------
Global Shake#

Global ExplosionTimer#, ExplosionSFX%

Global LightsOn% = True

Global SoundTransmission%

;menus, GUI ---------------------------------------------------------------------------------------------------------
Global MainMenuOpen%, MenuOpen%, StopHidingTimer#, InvOpen%
Global OtherOpen.Items = Null

Global SelectedEnding$, EndingScreen%, EndingTimer#

Global MsgTimer#, Msg$, DeathMSG$

Global AccessCode%, KeypadInput$, KeypadTimer#, KeypadMSG$

Global DrawHandIcon%
Dim DrawArrowIcon%(4)

;misc ---------------------------------------------------------------------------------------------------------------

Include "Difficulty.bb"

Global MTFtimer#, MTFrooms.Rooms[10], MTFroomState%[10]

Dim RadioState#(10)
Dim RadioCHN%(8)

Dim OldAiPics%(5)

Global PlayTime%

Global InfiniteStamina% = False

;[End block]


;----------------------------------------------  Console -----------------------------------------------------

Global ConsoleOpen%, ConsoleInput$

Type ConsoleMsg
	Field txt$
End Type

Function CreateConsoleMsg(txt$)
	Local c.ConsoleMsg = New ConsoleMsg
	Insert c Before First ConsoleMsg
	
	c\txt = txt
End Function

Function UpdateConsole()
	
	If ConsoleOpen Then
		Local x% = 20, y% = 20, width% = 400, height% = 500
		Local StrTemp$, temp%,  i%
		Local ev.Events, r.Rooms, it.Items
		
		DrawFrame x,y,width,height
		
		Color 255, 255, 255
		
		SelectedInputBox = 2
		ConsoleInput = InputBox(x, y + height - 30, width, 30, ConsoleInput, 2)
		ConsoleInput = Left(ConsoleInput, 50)
		
		If KeyHit(28) And ConsoleInput <> "" Then
			If Instr(ConsoleInput, " ") > 0 Then
				StrTemp$ = Lower(Left(ConsoleInput, Instr(ConsoleInput, " ") - 1))
			Else
				StrTemp$ = Lower(ConsoleInput)
			End If
			
			Select Lower(StrTemp)
				Case "help"
					If Instr(ConsoleInput, " ")<>0 Then
						StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Else
						StrTemp$ = ""
					EndIf
					
					Select Lower(StrTemp)
						Case "1",""
							CreateConsoleMsg("LIST OF COMMANDS - PAGE 1/2")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("- asd")
							CreateConsoleMsg("- status")
							CreateConsoleMsg("- camerapick")
							CreateConsoleMsg("- ending")
							CreateConsoleMsg("- noclipspeed")
							CreateConsoleMsg("- noclip")
							CreateConsoleMsg("- injure [value]")
							CreateConsoleMsg("- infect [value]")
							CreateConsoleMsg("- heal")
							CreateConsoleMsg("- teleport [room name]")
							CreateConsoleMsg("- spawnitem [item name]")
							CreateConsoleMsg("- wireframe")
							CreateConsoleMsg("- 173speed")
							CreateConsoleMsg("- 106speed")
							CreateConsoleMsg("- 173state")
							CreateConsoleMsg("- 106state")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Use "+Chr(34)+"help 2"+Chr(34)+" to find more commands.")
							CreateConsoleMsg("Use "+Chr(34)+"help [command name]"+Chr(34)+" to get more information about a command.")
							CreateConsoleMsg("******************************")
						Case "2"
							CreateConsoleMsg("LIST OF COMMANDS - PAGE 2/2")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("- spawn513-1")
							CreateConsoleMsg("- spawn106")
							CreateConsoleMsg("- reset096")
							CreateConsoleMsg("- disable173")
							CreateConsoleMsg("- enable173")
							CreateConsoleMsg("- disable106")
							CreateConsoleMsg("- enable106")
							CreateConsoleMsg("- halloween")
							CreateConsoleMsg("- sanic")
							CreateConsoleMsg("- scp-420-j")
							CreateConsoleMsg("- godmode")
							CreateConsoleMsg("- revive")
							CreateConsoleMsg("- noclip")
							CreateConsoleMsg("- showfps")
							CreateConsoleMsg("- 096state")
							CreateConsoleMsg("- debughud")
							CreateConsoleMsg("- camerafog [near] [far]")
							CreateConsoleMsg("- brightness [value]")
							CreateConsoleMsg("- spawn [npc type]")
							CreateConsoleMsg("- infinitestamina")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Use "+Chr(34)+"help [command name]"+Chr(34)+" to get more information about a command.")
							CreateConsoleMsg("******************************")
						Case "asd"
							CreateConsoleMsg("HELP - asd")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Actives godmode, noclip, wireframe and")
							CreateConsoleMsg("sets fog distance to 20 near, 30 far")
							CreateConsoleMsg("******************************")
						Case "noclip"
							CreateConsoleMsg("HELP - noclip")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Toggles noclip, unless a valid parameter")
							CreateConsoleMsg("is specified (on/off).")
							CreateConsoleMsg("******************************")
						Case "godmode"
							CreateConsoleMsg("HELP - godmode")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Toggles godmode, unless a valid parameter")
							CreateConsoleMsg("is specified (on/off).")
							CreateConsoleMsg("******************************")
						Case "wireframe"
							CreateConsoleMsg("HELP - wireframe")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Toggles wireframe, unless a valid parameter")
							CreateConsoleMsg("is specified (on/off).")
							CreateConsoleMsg("******************************")
						Case "spawnitem"
							CreateConsoleMsg("HELP - spawnitem")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Spawns an item at the player's location.")
							CreateConsoleMsg("Any name that can appear in your inventory")
							CreateConsoleMsg("is a valid parameter.")
							CreateConsoleMsg("Example: spawnitem Key Card Omni")
							CreateConsoleMsg("******************************")
						Case "spawn"
							CreateConsoleMsg("HELP - spawn")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Spawns an NPC at the player's location.")
							CreateConsoleMsg("Valid parameters are:")
							CreateConsoleMsg("mtf / 173 / 106 / guard / ")
							CreateConsoleMsg("096 / 049 / zombie / npc178")
							CreateConsoleMsg("******************************")
						Case "revive"
							CreateConsoleMsg("HELP - revive")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Resets the player's death timer.")
							CreateConsoleMsg("******************************")
						Case "teleport"
							CreateConsoleMsg("HELP - teleport")
							CreateConsoleMsg("******************************")
							CreateConsoleMsg("Teleports the player to the first instance")
							CreateConsoleMsg("of the specified room. Any room that appears")
							CreateConsoleMsg("in rooms.ini is a valid parameter.")
							CreateConsoleMsg("******************************")
						Default
							CreateConsoleMsg("There is no help available for that command")
					End Select
					
				Case "asd"
					WireFrame 1
					WireframeState=1
					GodMode = 1
					NoClip = 1
					CameraFogNear = 15
					CameraFogFar = 20
				Case "mute"
					For e.events = Each Events
						If e\eventname = "alarm" Then 
							StopChannel e\soundchn
							e\SoundCHN = 0
							StopChannel e\soundchn2
							e\SoundCHN2 = 0
							e\eventstate = 4000
							Exit
						EndIf
					Next
				Case "status"
					CreateConsoleMsg("******************************")
					CreateConsoleMsg("Status: ")
					CreateConsoleMsg("Coordinates: ")
					CreateConsoleMsg("    - collider: "+EntityX(Collider)+", "+EntityY(Collider)+", "+EntityZ(Collider))
					CreateConsoleMsg("    - camera: "+EntityX(Camera)+", "+EntityY(Camera)+", "+EntityZ(Camera))
					
					CreateConsoleMsg("Rotation: ")
					CreateConsoleMsg("    - collider: "+EntityPitch(Collider)+", "+EntityYaw(Collider)+", "+EntityRoll(Collider))
					CreateConsoleMsg("    - camera: "+EntityPitch(Camera)+", "+EntityYaw(Camera)+", "+EntityRoll(Camera))
					
					CreateConsoleMsg("Room: "+PlayerRoom\RoomTemplate\Name)
					For ev.Events = Each Events
						If ev\room = PlayerRoom Then
							CreateConsoleMsg("Room event: "+ev\EventName)	
							CreateConsoleMsg("-    state: "+ev\EventState)
							CreateConsoleMsg("-    state2: "+ev\EventState2)	
							CreateConsoleMsg("-    state3: "+ev\EventState3)
							Exit
						EndIf
					Next
					
					CreateConsoleMsg("Room coordinates: "+Floor(EntityX(PlayerRoom\obj) / 8.0 + 0.5)+", "+ Floor(EntityZ(PlayerRoom\obj) / 8.0 + 0.5))
					CreateConsoleMsg("Stamina: "+Stamina)
					CreateConsoleMsg("Death timer: "+KillTimer)					
					CreateConsoleMsg("Blinktimer: "+BlinkTimer)
					CreateConsoleMsg("Injuries: "+Injuries)
					CreateConsoleMsg("Bloodloss: "+Bloodloss)
					CreateConsoleMsg("******************************")
				Case "camerapick"
					c = CameraPick(Camera,GraphicWidth/2, GraphicHeight/2)
					If c = 0 Then
						CreateConsoleMsg("******************************")
						CreateConsoleMsg("No entity  picked")
						CreateConsoleMsg("******************************")								
					Else
						CreateConsoleMsg("******************************")
						CreateConsoleMsg("Picked entity:")
						sf = GetSurface(c,1)
						b = GetSurfaceBrush( sf )
						t = GetBrushTexture(b,0)
						texname$ =  StripPath(TextureName(t))
						CreateConsoleMsg("Texture name: "+texname)
						CreateConsoleMsg("Coordinates:"+EntityX(c)+", "+EntityY(c)+", "+EntityZ(c))
						CreateConsoleMsg("******************************")							
					EndIf
				Case "hidedistance"
					HideDistance = Float(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					CreateConsoleMsg("Hidedistance set to"+HideDistance)					
				Case "ending"
					SelectedEnding = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					KillTimer = -0.1
					EndingTimer = -0.1
				Case "noclipspeed"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					NoClipSpeed = Float(StrTemp)
				Case "injure"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Injuries = Float(StrTemp)
				Case "infect"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Infect = Float(StrTemp)
				Case "heal"
					Injuries = 0
					Bloodloss = 0
				Case "teleport"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "895", "scp-895"
							StrTemp = "coffin"
						Case "scp-914"
							StrTemp = "914"
						Case "offices", "office"
							StrTemp = "room2offices"
					End Select
					
					For r.Rooms = Each Rooms
						If r\RoomTemplate\Name = StrTemp Then
							PositionEntity (Collider, EntityX(r\obj), 0.7, EntityZ(r\obj))
							ResetEntity(Collider)
							UpdateDoors()
							UpdateRooms()
							For it.Items = Each Items
								it\disttimer = 0
							Next
							PlayerRoom = r
							Exit
						EndIf
					Next
					
					If PlayerRoom\RoomTemplate\Name <> StrTemp Then CreateConsoleMsg("Room not found")
				Case "spawnitem"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					temp = False 
					For itt.Itemtemplates = Each ItemTemplates
						If (Lower(itt\name) = StrTemp) Then
							temp = True
							CreateConsoleMsg(itt\name + " spawned")
							it.Items = CreateItem(itt\name, itt\tempname, EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
							EntityType(it\obj, HIT_ITEM)
							Exit
						Else If (Lower(itt\tempname) = StrTemp) Then
							temp = True
							CreateConsoleMsg(itt\name + " spawned")
							it.Items = CreateItem(itt\name, itt\tempname, EntityX(Collider), EntityY(Camera,True), EntityZ(Collider))
							EntityType(it\obj, HIT_ITEM)
							Exit
						End If
					Next
					
					If temp = False Then CreateConsoleMsg("Item not found")
				Case "wireframe"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							WireframeState = True 
							CreateConsoleMsg("WIREFRAME ON")							
						Case "off", "0", "false"
							WireframeState = False
							CreateConsoleMsg("WIREFRAME OFF")
						Default
							WireframeState = Not WireframeState
							If WireframeState = False Then
								CreateConsoleMsg("WIREFRAME OFF")
							Else
								CreateConsoleMsg("WIREFRAME ON")	
							EndIf
					End Select
					
					WireFrame WireframeState
				Case "173speed"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Curr173\Speed = Float(StrTemp)
					CreateConsoleMsg("173's speed set to " + StrTemp)
				Case "106speed"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Curr106\Speed = Float(StrTemp)
					CreateConsoleMsg("106's speed set to " + StrTemp)
				Case "173state"
					CreateConsoleMsg("SCP-173")
					CreateConsoleMsg("Position: " + EntityX(Curr173\obj) + ", " + EntityY(Curr173\obj) + ", " + EntityZ(Curr173\obj))
					CreateConsoleMsg("Idle: " + Curr173\Idle)
					CreateConsoleMsg("State: " + Curr173\State)
				Case "106state"
					CreateConsoleMsg("SCP-106")
					CreateConsoleMsg("Position: " + EntityX(Curr106\obj) + ", " + EntityY(Curr106\obj) + ", " + EntityZ(Curr106\obj))
					CreateConsoleMsg("Idle: " + Curr106\Idle)
					CreateConsoleMsg("State: " + Curr106\State)
				Case "spawn513-1"
					CreateNPC(NPCtype5131, 0,0,0)
				Case "spawn106"
					Curr106\State = -1
					PositionEntity Curr106\Collider, EntityX(Collider), EntityY(Curr106\Collider), EntityZ(Collider)
				Case "reset096"
					For n.NPCs = Each NPCs
						If n\NPCtype = NPCtype096 Then
							RemoveNPC(n)
							CreateEvent("lockroom096", "lockroom2", 0)   
							Exit
						EndIf
					Next
				Case "disable173"
					Curr173\Idle = True
					Disabled173=True
				Case "enable173"
					Curr173\Idle = False
					Disabled173=False
					ShowEntity Curr173\obj
					ShowEntity Curr173\Collider
				Case "disable106"
					Curr106\Idle = True
					Curr106\State = 200000
					Contained106 = True
				Case "enable106"
					Curr106\Idle = False
				Case "halloween"
					Local tex = LoadTexture("GFX\npcs\173h.pt")
					EntityTexture Curr173\obj, tex, 0, 2
					FreeTexture tex
				Case "sanic"
					SuperMan = Not SuperMan
					If SuperMan = True Then
						CreateConsoleMsg("GOTTA GO FAST")
					Else
						CreateConsoleMsg("WHOA SLOW DOWN")
					EndIf
				Case "scp-420-j","420","weed"
					For i = 1 To 20
						If Rand(2)=1 Then
							it.Items = CreateItem("Some SCP-420-J","420", EntityX(Collider,True)+Cos((360.0/20.0)*i)*Rnd(0.3,0.5), EntityY(Camera,True), EntityZ(Collider,True)+Sin((360.0/20.0)*i)*Rnd(0.3,0.5))
						Else
							it.Items = CreateItem("Joint","420s", EntityX(Collider,True)+Cos((360.0/20.0)*i)*Rnd(0.3,0.5), EntityY(Camera,True), EntityZ(Collider,True)+Sin((360.0/20.0)*i)*Rnd(0.3,0.5))
						EndIf
						EntityType (it\obj, HIT_ITEM)
					Next
					PlaySound_Strict LoadTempSound("SFX\Mandeville.ogg")
				Case "godmode"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							GodMode = True
							CreateConsoleMsg("GODMODE ON")							
						Case "off", "0", "false"
							GodMode = False
							CreateConsoleMsg("GODMODE OFF")	
						Default
							GodMode = Not GodMode
							If GodMode = False Then
								CreateConsoleMsg("GODMODE OFF")
							Else
								CreateConsoleMsg("GODMODE ON")	
							EndIf
					End Select	
				Case "revive","undead","resurrect"
					DropSpeed = -0.1
					HeadDropSpeed = 0.0
					Shake = 0
					CurrSpeed = 0
					
					HeartBeatVolume = 0
					
					CameraShake = 0
					Shake = 0
					LightFlash = 0
					BlurTimer = 0
					
					FallTimer = 0
					MenuOpen = False
					
					GodMode = 0
					NoClip = 0
					
					ShowEntity Collider
					
					KillTimer = 0
					KillAnim = 0
					
				Case "noclip","fly"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							NoClip = True
							CreateConsoleMsg("NOCLIP ON")	
							Playable = True
						Case "off", "0", "false"
							NoClip = False
							CreateConsoleMsg("NOCLIP OFF")		
							RotateEntity Collider, 0, EntityYaw(Collider), 0
						Default
							NoClip = Not NoClip
							If NoClip = False Then
								CreateConsoleMsg("NOCLIP OFF")		
								RotateEntity Collider, 0, EntityYaw(Collider), 0
							Else
								CreateConsoleMsg("NOCLIP ON")	
								Playable = True
							EndIf
					End Select
					
					DropSpeed = 0
					
				Case "showfps"
					ShowFPS = Not ShowFPS
					CreateConsoleMsg("ShowFPS: "+Str(ShowFPS))
					
				Case "096state"
					For n.NPCs = Each NPCs
						If n\NPCtype = NPCtype096 Then
							CreateConsoleMsg("SCP-096")
							CreateConsoleMsg("Position: " + EntityX(n\obj) + ", " + EntityY(n\obj) + ", " + EntityZ(n\obj))
							CreateConsoleMsg("Idle: " + n\Idle)
							CreateConsoleMsg("State: " + n\State)
							Exit
						EndIf
					Next
					CreateConsoleMsg("SCP-096 has not spawned")
					
				Case "debughud"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Select StrTemp
						Case "on", "1", "true"
							DebugHUD = True
							CreateConsoleMsg("Debug Mode On")
						Case "off", "0", "false"
							DebugHUD = False
							CreateConsoleMsg("Debug Mode Off")
						Default
							DebugHUD = Not DebugHUD
							If DebugHUD = False Then
								CreateConsoleMsg("Debug Mode Off")
							Else
								CreateConsoleMsg("Debug Mode On")
							EndIf
					End Select
					
				Case "camerafog"
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					CameraFogNear = Float(Left(args, Len(args) - Instr(args, " ")))
					CameraFogFar = Float(Right(args, Len(args) - Instr(args, " ")))
					CreateConsoleMsg("Near set to: " + CameraFogNear + ", far set to: " + CameraFogFar)
					
				Case "brightness"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Brightness = Int(StrTemp)
					CreateConsoleMsg("Brightness set to " + Brightness)
				Case "spawn"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					Select StrTemp 
						Case "mtf"
							n.NPCs = CreateNPC(NPCtypeMTF, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))
						Case "173","scp173","scp-173"
							n.NPCs = CreateNPC(NPCtype173, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))
						Case "106","scp106","scp-106","larry"
							n.NPCs = CreateNPC(NPCtypeOldMan, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))	
						Case "guard"
							n.NPCs = CreateNPC(NPCtypeGuard, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))		
						Case "096","scp096","scp-096"
							n.NPCs = CreateNPC(NPCtype096, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))			
						Case "049","scp049","scp-049"
							n.NPCs = CreateNPC(NPCtype049, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))		
							n\state = 2
						Case "zombie","scp-049-2"
							n.NPCs = CreateNPC(NPCtypeZombie, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))			
							n\state = 1
						Case "966", "scp966", "scp-966"
							n.NPCs = CreateNPC(NPCtype966, EntityX(Collider),EntityY(Collider)+0.2,EntityZ(Collider))
						Default 
							CreateConsoleMsg("NPC type not found")
					End Select
				;new Console Commands in SCP:CB 1.3 - ENDSHN
				Case "infinitestamina"
					StrTemp$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					
					Select StrTemp
						Case "on", "1", "true"
							InfiniteStamina% = True
							CreateConsoleMsg("INFINITE STAMINA ON")							
						Case "off", "0", "false"
							InfiniteStamina% = False
							CreateConsoleMsg("INFINITE STAMINA OFF")	
						Default
							InfiniteStamina% = Not InfiniteStamina%
							If InfiniteStamina% = False Then
								CreateConsoleMsg("INFINITE STAMINA OFF")
							Else
								CreateConsoleMsg("INFINITE STAMINA ON")	
							EndIf
					End Select
				Case "asd2"
					GodMode = 1
					InfiniteStamina = 1
					Curr173\Idle = True
					Disabled173=True
					Curr106\Idle = True
					Curr106\State = 200000
					Contained106 = True
				Case "spawnnpcstate"
					args$ = Lower(Right(ConsoleInput, Len(ConsoleInput) - Instr(ConsoleInput, " ")))
					StrTemp$ = Piece$(args$,1," ")
					StrTemp2$ = Piece$(args$,2," ")
					
					Console_SpawnNPC(StrTemp$,Int(StrTemp2$))
				Default
					CreateConsoleMsg("Command not found")
			End Select
			
			ConsoleInput = ""
		End If
		
		Local TempY% = y + height - 70
		Local cm.ConsoleMsg
		For cm.ConsoleMsg = Each ConsoleMsg
			If TempY < y + 20 Then
				Delete cm
			Else
				Text(x + 20, TempY, cm\txt)
				TempY = TempY - 15
			EndIf
		Next
	End If
	
End Function


CreateConsoleMsg("Console commands: ")
CreateConsoleMsg("  - teleport [room name]")
CreateConsoleMsg("  - godmode [on/off]")
CreateConsoleMsg("  - noclip [on/off]")
CreateConsoleMsg("  - noclipspeed [x] (default = 2.0)")
CreateConsoleMsg("  - wireframe [on/off]")
CreateConsoleMsg("  - debughud [on/off]")
CreateConsoleMsg("  - camerafog [near] [far]")
CreateConsoleMsg(" ")
CreateConsoleMsg("  - status")
CreateConsoleMsg("  - heal")
CreateConsoleMsg(" ")
CreateConsoleMsg("  - spawnitem [item name]")
CreateConsoleMsg(" ")
CreateConsoleMsg("  - 173speed [x] (default = 35)")
CreateConsoleMsg("  - disable173/enable173")
CreateConsoleMsg("  - disable106/enable106")
CreateConsoleMsg("  - 173state/106state/096state")
CreateConsoleMsg("  - spawn [npc type]")

;---------------------------------------------------------------------------------------------------

Global DebugHUD%

Global BlurVolume#, BlurTimer#

Global LightBlink#, LightFlash#

Global BumpEnabled% = GetINIInt("options.ini", "options", "bump mapping enabled")
Global HUDenabled% = GetINIInt("options.ini", "options", "HUD enabled")

Global Camera%, CameraShake#, CurrCameraZoom#

Global Brightness% = 40
Global CameraFogNear# = GetINIFloat("options.ini", "options", "camera fog near")
Global CameraFogFar# = GetINIFloat("options.ini", "options", "camera fog far")

Global StoredCameraFogFar# = CameraFogFar

Global MouseSens# = GetINIFloat("options.ini", "options", "mouse sensitivity")

Include "dreamfilter.bb"

Dim LightSpriteTex(10)

;----------------------------------------------  Sounds -----------------------------------------------------

;[Block]

Global SoundEmitter%
Global TempSounds%[10]
Global TempSoundCHN%
Global TempSoundIndex% = 0


Dim Music%(40)
Music(0) = LoadSound_Strict("SFX\Music\The Dread.ogg")
Music(1) = LoadSound_Strict("SFX\Music\HCZ Background.ogg") 
Music(2) = LoadSound_Strict("SFX\Music\Anxiety.ogg") 
;Music(3) = LoadSound_Strict("SFX\Ambient\PocketDimension.ogg")
;Music(4) = LoadSound_Strict("SFX\Music\AI.ogg")
;Music(5) = LoadSound_Strict("SFX\Music\Satiate Strings.ogg")
;Music(6) = LoadSound_Strict("SFX\Music\Medusa.ogg")
;Music(7) = LoadSound_Strict("SFX\Music\Groaning Ambience.ogg") 
;Music(8) = LoadSound_Strict("SFX\Music\SCP-049 Tension.ogg") 
;Music(9) = LoadSound_Strict("SFX\Music\Forest.ogg") 
Music(10) = LoadSound_Strict("SFX\Music\Bump in the Night.ogg")
Music(11) = LoadSound_Strict("SFX\Music\MenuAmbience.ogg")
;Music(12) = LoadSound_strict("SFX\Music\Forest2.ogg")
;Music(13) = LoadSound_strict("SFX\Music\Blue Feather.ogg")
;Music(14) = LoadSound("SFX\178ambient.ogg")
;Music(15) = LoadSound("SFX\Music\PDTrenchAmbience.ogg")
;Music(15) = LoadSound("SFX\Music\205_music.ogg")


Global MusicVolume# = GetINIFloat(OptionFile, "options", "music volume")
Global MusicCHN% = PlaySound_Strict(Music(2))
ChannelVolume(MusicCHN, MusicVolume)
Global CurrMusicVolume# = 1.0, NowPlaying%=2, ShouldPlay%=11

DrawLoading(10, True)

Dim OpenDoorSFX%(3,3), CloseDoorSFX%(3,3)
For i = 0 To 2
	OpenDoorSFX(0,i) = LoadSound_Strict("SFX\Doors\DoorOpen" + (i + 1) + ".ogg")
	CloseDoorSFX(0,i) = LoadSound_Strict("SFX\Doors\DoorClose" + (i + 1) + ".ogg")
	OpenDoorSFX(2,i) = LoadSound_Strict("SFX\Doors\Door2Open" + (i + 1) + ".ogg")
	CloseDoorSFX(2,i) = LoadSound_Strict("SFX\Doors\Door2Close" + (i + 1) + ".ogg")
Next
OpenDoorSFX(1,0) = LoadSound_Strict("SFX\Doors\BigDoorOpen.ogg")
CloseDoorSFX(1,0) = LoadSound_Strict("SFX\Doors\BigDoorClose.ogg")

Global KeyCardSFX1 = LoadSound_Strict("SFX\Doors\KeyCardUse1.ogg"), KeyCardSFX2 = LoadSound_Strict("SFX\Doors\KeyCardUse2.ogg")

Global OpenDoorFastSFX=LoadSound_Strict("SFX\Doors\DoorOpenFast.ogg")
Global CautionSFX% = LoadSound_Strict("SFX\caution.ogg")

Global NuclearSirenSFX%

Global CameraSFX = LoadSound_Strict("SFX\camera.ogg") 

Global StoneDragSFX% = LoadSound_Strict("SFX\StoneDrag.ogg")

Global GunshotSFX% = LoadSound_Strict("SFX\gunshot.ogg"),Gunshot2SFX% = LoadSound_Strict("SFX\gunshot2.ogg"),Gunshot3SFX% = LoadSound_Strict("SFX\bulletmiss.ogg")
Global BullethitSFX% = LoadSound_Strict("SFX\bullethit.ogg")

Global TeslaIdleSFX = LoadSound_Strict("SFX\teslaidle.ogg"), TeslaActivateSFX = LoadSound_Strict("SFX\teslaactivate.ogg")
Global TeslaPowerUpSFX = LoadSound_Strict("SFX\teslapowerup.ogg")

Global MagnetUpSFX% = LoadSound_Strict("SFX\MagnetUp.ogg"), MagnetDownSFX = LoadSound_Strict("SFX\MagnetDown.ogg")
Global FemurBreakerSFX%

Dim DecaySFX%(5)
For i = 0 To 3
	DecaySFX(i) = LoadSound_Strict("SFX\decay" + i + ".ogg")
Next

Global BurstSFX = LoadSound_Strict("SFX\burst.ogg")

DrawLoading(20, True)

Dim RustleSFX%(3)
For i = 0 To 2
	RustleSFX(i) = LoadSound_Strict("SFX\rustle" + i + ".ogg")
Next

Global Death914SFX% = LoadSound_Strict("SFX\914death.ogg"), Use914SFX% = LoadSound_Strict("SFX\914use.ogg")

Dim DripSFX%(4)
For i = 0 To 3
	DripSFX(i) = LoadSound_Strict("SFX\drip" + i + ".ogg")
Next

Global LeverSFX% = LoadSound_Strict("SFX\lever.ogg"), LightSFX% = LoadSound_Strict("SFX\lightswitch.ogg")

;Global GasmaskBreathCHN%, GasmaskBreath% = LoadSound_Strict("SFX\GasmaskBreath.ogg")

Global ButtGhostSFX% = LoadSound_Strict("SFX\BuGh.ogg")

Dim RadioSFX(5,10)
RadioSFX(1,0) = LoadSound_Strict("SFX\Radio\RadioAlarm.ogg")
RadioSFX(1,1) = LoadSound_Strict("SFX\Radio\RadioAlarm2.ogg")
For i = 0 To 8
	RadioSFX(2,i) = LoadSound_Strict("SFX\Radio\scpradio"+i+".ogg")
Next
Global RadioSquelch = LoadSound_Strict("SFX\Radio\squelch.ogg")
Global RadioStatic = LoadSound_Strict("SFX\Radio\static.ogg")
Global RadioBuzz = LoadSound_Strict("SFX\Radio\buzz.ogg")

Global ElevatorBeepSFX = LoadSound_Strict("SFX\ElevatorBeep.ogg"), ElevatorMoveSFX = LoadSound_Strict("SFX\ElevatorMove.ogg") 

Dim PickSFX%(10)
For i = 0 To 3
	PickSFX(i) = LoadSound_Strict("SFX\PickItem" + i + ".ogg")
Next

Global AmbientSFXCHN%, CurrAmbientSFX%
Dim AmbientSFXAmount(6)
;0 = light containment, 1 = heavy containment, 2 = entrance
AmbientSFXAmount(0)=8 : AmbientSFXAmount(1)=11 : AmbientSFXAmount(2)=12
;3 = general, 4 = pre-breach
AmbientSFXAmount(3)=15 : AmbientSFXAmount(4)=3
;5 = forest
AmbientSFXAmount(5)=10

Dim AmbientSFX%(6, 15)

Dim OldManSFX%(6)
For i = 0 To 4
	OldManSFX(i) = LoadSound_Strict("SFX\oldman" + (i + 1) + ".ogg")
Next
OldManSFX(5) = LoadSound_Strict("SFX\oldmandrag.ogg")

Dim Scp173SFX%(3)
For i = 0 To 2
	Scp173SFX(i) = LoadSound_Strict("SFX\173sound" + (i + 1) + ".ogg")
Next

Dim HorrorSFX%(20)
For i = 0 To 10
	HorrorSFX(i) = LoadSound_Strict("SFX\horror" + i + ".ogg")
Next
For i = 14 To 15
	HorrorSFX(i) = LoadSound_Strict("SFX\horror" + i + ".ogg")
Next

DrawLoading(25, True)

Dim IntroSFX%(20)

For i = 7 To 9
	IntroSFX(i) = LoadSound_Strict("SFX\intro\bang" + (i - 6) + ".ogg")
Next
For i = 10 To 12
	IntroSFX(i) = LoadSound_Strict("SFX\intro\light" + (i - 9) + ".ogg")
Next
;IntroSFX(13) = LoadSound_Strict("SFX\intro\shoot1.ogg")
;IntroSFX(14) = LoadSound_Strict("SFX\intro\shoot2.ogg")
IntroSFX(15) = LoadSound_Strict("SFX\intro\metal173.ogg")

Dim AlarmSFX%(5)
AlarmSFX(0) = LoadSound_Strict("SFX\alarm.ogg")
AlarmSFX(1) = LoadSound_Strict("SFX\alarm2.ogg")
AlarmSFX(2) = LoadSound_Strict("SFX\alarm3.ogg")

Global HeartBeatSFX = LoadSound_Strict("SFX\heartbeat.ogg")

Dim BreathSFX(2,5)
Global BreathCHN%
For i = 0 To 4
	BreathSFX(0,i)=LoadSound_Strict("SFX\9431\breath"+i+".ogg")
	BreathSFX(1,i)=LoadSound_Strict("SFX\9431\breath"+i+"gas.ogg")
Next


Dim NeckSnapSFX(3)
For i = 0 To 2
	NeckSnapSFX(i) =  LoadSound_Strict("SFX\necksnap"+(i+1)+".ogg")
Next

Dim DamageSFX%(9)
For i = 0 To 8
	DamageSFX(i) = LoadSound_Strict("SFX\Damage"+(i+1)+".ogg")
Next

Dim MTFSFX%(8)

Dim CoughSFX%(3)
Global CoughCHN%
For i = 0 To 2
	CoughSFX(i) = LoadSound_Strict("SFX\cough" + (i + 1) + ".ogg")
Next

Global MachineSFX% = LoadSound_Strict("SFX\Machine.ogg")

Global ApacheSFX = LoadSound_Strict("SFX\apache.ogg")

Global CurrStepSFX
Dim StepSFX%(3, 2, 4) ;(normal/metal, walk/run, id)
For i = 0 To 3
	StepSFX(0, 0, i) = LoadSound_Strict("SFX\step" + (i + 1) + ".ogg")
	StepSFX(1, 0, i) = LoadSound_Strict("SFX\stepmetal" + (i + 1) + ".ogg")
	StepSFX(0, 1, i)= LoadSound_Strict("SFX\run" + (i + 1) + ".ogg")
	StepSFX(1, 1, i) = LoadSound_Strict("SFX\runmetal" + (i + 1) + ".ogg")
	If i < 3 Then StepSFX(2, 0, i) = LoadSound_Strict("SFX\MTF\StepMTF" + (i + 1) + ".ogg")	
Next

Dim Step2SFX(6)
For i = 0 To 2
	Step2SFX(i) = LoadSound_Strict("SFX\stepPD" + (i + 1) + ".ogg")
	Step2SFX(i+3) = LoadSound_Strict("SFX\stepForest" + (i + 1) + ".ogg")
Next 

DrawLoading(30, True)

;[End block]

;New Sounds in SCP:CB 1.3 - ENDSHN
;[Block]
Global NTF_1499EnterSFX% = LoadSound_Strict("SFX\1499\1499_mfe_vhd_00.ogg")
Global NTF_1499LeaveSFX% = LoadSound_Strict("SFX\1499\1499_mfe_lve_10.ogg")
Global NTF_1499FuckedSFX% = LoadSound_Strict("SFX\1499\fuckedup.ogg")
;[End Block]

;-----------------------------------------  Images ----------------------------------------------------------

Global PauseMenuIMG% = LoadImage_Strict("GFX\menu\pausemenu.jpg")
MaskImage PauseMenuIMG, 255,255,0
ScaleImage PauseMenuIMG,MenuScale,MenuScale

Global SprintIcon% = LoadImage_Strict("GFX\sprinticon.png"), BlinkIcon% = LoadImage_Strict("GFX\blinkicon.png"), CrouchIcon% = LoadImage_Strict("GFX\sneakicon.png")
Global HandIcon% = LoadImage_Strict("GFX\handsymbol.png")

Global StaminaMeterIMG% = LoadImage_Strict("GFX\staminameter.jpg")

Global KeypadHUD =  LoadImage_Strict("GFX\keypadhud.jpg")
MaskImage(KeypadHUD, 255,0,255)

Global Panel294 = LoadImage_Strict("GFX\294panel.jpg"), Using294%, Input294$
MaskImage(Panel294, 255,0,255)

DrawLoading(35, True)

Global NTF_Wearing1499%
Global NTF_1499PrevX#
Global NTF_1499PrevY#
Global NTF_1499PrevZ#
Global NTF_1499PrevRoom$
Global NTF_1499X#
Global NTF_1499Y#
Global NTF_1499Z#
Global NTF_PrevPlayerRoom$

;----------------------------------------------  Items  -----------------------------------------------------

Include "Items.bb"

;--------------------------------------- Particles ------------------------------------------------------------

Include "Particles.bb"

;-------------------------------------  Doors --------------------------------------------------------------

Global ClosestButton%, ClosestDoor.Doors
Global SelectedDoor.Doors, UpdateDoorsTimer#
Global DoorTempID%
Type Doors
	Field obj%, obj2%, frameobj%, buttons%[2]
	Field locked%, open%, angle%, openstate#, fastopen%
	Field dir%
	Field timer%, timerstate#
	Field KeyCard%
	Field room.Rooms
	
	Field DisableWaypoint%
	
	Field dist#
	
	Field SoundCHN%
	
	Field Code$
	
	Field ID%
	
	Field Level%
	Field LevelDest%
	
	Field AutoClose%
	
	Field LinkedDoor.Doors
	
	Field IsElevatorDoor% = False
End Type 

Dim BigDoorOBJ(2), HeavyDoorObj(2)

Function CreateDoor.Doors(lvl, x#, y#, z#, angle#, room.Rooms, dopen% = False,  big% = False, keycard% = False, code$="")
	Local d.Doors, parent, i%
	If room <> Null Then parent = room\obj
	
	d.Doors = New Doors
	If big=1 Then
		d\obj = CopyEntity(BigDoorOBJ(0))
		ScaleEntity(d\obj, 55 * RoomScale, 55 * RoomScale, 55 * RoomScale)
		d\obj2 = CopyEntity(BigDoorOBJ(1))
		ScaleEntity(d\obj2, 55 * RoomScale, 55 * RoomScale, 55 * RoomScale)
		
		d\frameobj = CopyEntity(DoorColl)	;CopyMesh				
		ScaleEntity(d\frameobj, RoomScale, RoomScale, RoomScale)
		EntityType d\frameobj, HIT_MAP
		EntityAlpha d\frameobj, 0.0
	ElseIf big=2
		d\obj = CopyEntity(HeavyDoorObj(0))
		ScaleEntity(d\obj, RoomScale, RoomScale, RoomScale)
		d\obj2 = CopyEntity(HeavyDoorObj(1))
		ScaleEntity(d\obj2, RoomScale, RoomScale, RoomScale)
		
		d\frameobj = CopyEntity(DoorFrameOBJ)
	Else
		d\obj = CopyEntity(DoorOBJ)
		ScaleEntity(d\obj, (204.0 * RoomScale) / MeshWidth(d\obj), 312.0 * RoomScale / MeshHeight(d\obj), 16.0 * RoomScale / MeshDepth(d\obj))
		
		d\frameobj = CopyEntity(DoorFrameOBJ)
		d\obj2 = CopyEntity(DoorOBJ)
		
		ScaleEntity(d\obj2, (204.0 * RoomScale) / MeshWidth(d\obj), 312.0 * RoomScale / MeshHeight(d\obj), 16.0 * RoomScale / MeshDepth(d\obj))
		;entityType d\obj2, HIT_MAP
	End If
	
	;scaleentity(d\obj, 0.1, 0.1, 0.1)
	PositionEntity d\frameobj, x, y, z	
	ScaleEntity(d\frameobj, (8.0 / 2048.0), (8.0 / 2048.0), (8.0 / 2048.0))
	EntityType d\obj, HIT_MAP
	EntityType d\obj2, HIT_MAP
	
	d\ID = DoorTempID
	DoorTempID=DoorTempID+1
	
	d\KeyCard = keycard
	d\Code = code
	
	d\Level = lvl
	d\LevelDest = 66
	
	For i% = 0 To 1
		If code <> "" Then 
			d\buttons[i]= CopyEntity(ButtonCodeOBJ)
			EntityFX(d\buttons[i], 1)
		Else
			If keycard>0 Then
				d\buttons[i]= CopyEntity(ButtonKeyOBJ)
			ElseIf keycard<0
				d\buttons[i]= CopyEntity(ButtonScannerOBJ)	
			Else
				d\buttons[i] = CopyEntity(ButtonOBJ)
			End If
		EndIf
		
		ScaleEntity(d\buttons[i], 0.03, 0.03, 0.03)
	Next
	
	If big=1 Then
		PositionEntity d\buttons[0], x - 432.0 * RoomScale, y + 0.7, z + 192.0 * RoomScale
		PositionEntity d\buttons[1], x + 432.0 * RoomScale, y + 0.7, z - 192.0 * RoomScale
		RotateEntity d\buttons[0], 0, 90, 0
		RotateEntity d\buttons[1], 0, 270, 0
	Else
		PositionEntity d\buttons[0], x + 0.6, y + 0.7, z - 0.1
		PositionEntity d\buttons[1], x - 0.6, y + 0.7, z + 0.1
		RotateEntity d\buttons[1], 0, 180, 0		
	End If
	EntityParent(d\buttons[0], d\frameobj)
	EntityParent(d\buttons[1], d\frameobj)
	EntityPickMode(d\buttons[0], 2)
	EntityPickMode(d\buttons[1], 2)
	
	PositionEntity d\obj, x, y, z
	
	RotateEntity d\obj, 0, angle, 0
	RotateEntity d\frameobj, 0, angle, 0
	
	If d\obj2 <> 0 Then
		PositionEntity d\obj2, x, y, z
		If big=1 Then
			RotateEntity(d\obj2, 0, angle, 0)
		Else
			RotateEntity(d\obj2, 0, angle + 180, 0)
		EndIf
		EntityParent(d\obj2, parent)
	EndIf
	
	EntityParent(d\frameobj, parent)
	EntityParent(d\obj, parent)
	
	d\angle = angle
	d\open = dopen		
	
	EntityPickMode(d\obj, 3)
	MakeCollBox(d\obj)
	If d\obj2 <> 0 Then
		EntityPickMode(d\obj2, 3)
		MakeCollBox(d\obj2)
	End If
	
	EntityPickMode d\frameobj,2
	
	If d\open And big = False And Rand(8) = 1 Then d\AutoClose = True
	d\dir=big
	d\room=room
	
	Return d
	
End Function

Function CreateButton(x#,y#,z#, pitch#,yaw#,roll#=0)
	Local obj = CopyEntity(ButtonOBJ)	
	
	ScaleEntity(obj, 0.03, 0.03, 0.03)
	
	PositionEntity obj, x,y,z
	RotateEntity obj, pitch,yaw,roll
	
	EntityPickMode(obj, 2)	
	
	Return obj
End Function

Function UpdateDoors()
	
	Local i%, d.Doors, x#, z#, dist#
	If UpdateDoorsTimer =< 0 Then
		For d.Doors = Each Doors
			Local xdist# = Abs(EntityX(Collider)-EntityX(d\obj,True))
			Local zdist# = Abs(EntityZ(Collider)-EntityZ(d\obj,True))
			
			d\dist = xdist+zdist
			
			If d\dist > HideDistance*2 Then
				If d\obj <> 0 Then HideEntity d\obj
				If d\frameobj <> 0 Then HideEntity d\frameobj
				If d\obj2 <> 0 Then HideEntity d\obj2
				If d\buttons[0] <> 0 Then HideEntity d\buttons[0]
				If d\buttons[1] <> 0 Then HideEntity d\buttons[1]				
			Else
				If d\obj <> 0 Then ShowEntity d\obj
				If d\frameobj <> 0 Then ShowEntity d\frameobj
				If d\obj2 <> 0 Then ShowEntity d\obj2
				If d\buttons[0] <> 0 Then ShowEntity d\buttons[0]
				If d\buttons[1] <> 0 Then ShowEntity d\buttons[1]							
			EndIf
			
		Next
		
		UpdateDoorsTimer = 30
	Else
		UpdateDoorsTimer = Max(UpdateDoorsTimer-FPSfactor,0)
	EndIf
	
	ClosestButton = 0
	ClosestDoor = Null
	
	For d.Doors = Each Doors
		If d\dist < HideDistance*2 Then 
			
			If (d\openstate >= 180 Or d\openstate <= 0) And GrabbedEntity = 0 Then
				For i% = 0 To 1
					If d\buttons[i] <> 0 Then
						If Abs(EntityX(Collider)-EntityX(d\buttons[i],True)) < 1.0 Then 
							If Abs(EntityZ(Collider)-EntityZ(d\buttons[i],True)) < 1.0 Then 
								dist# = Distance(EntityX(Collider, True), EntityZ(Collider, True), EntityX(d\buttons[i], True), EntityZ(d\buttons[i], True));entityDistance(collider, d\buttons[i])
								If dist < 0.7 Then
									Local temp% = CreatePivot()
									PositionEntity temp, EntityX(Camera), EntityY(Camera), EntityZ(Camera)
									PointEntity temp,d\buttons[i]
									
									If EntityPick(temp, 0.6) = d\buttons[i] Then
										If ClosestButton = 0 Then
											ClosestButton = d\buttons[i]
											ClosestDoor = d
										Else
											If dist < EntityDistance(Collider, ClosestButton) Then ClosestButton = d\buttons[i] : ClosestDoor = d
										End If							
									End If
									
									FreeEntity temp
									
								EndIf							
							EndIf
						EndIf
						
					EndIf
				Next
			EndIf
			
			If d\open Then
				If d\openstate < 180 Then
					Select d\dir
						Case 0
							d\openstate = Min(180, d\openstate + FPSfactor * 2 * (d\fastopen+1))
							MoveEntity(d\obj, Sin(d\openstate) * (d\fastopen*2+1) * FPSfactor / 80.0, 0, 0)
							If d\obj2 <> 0 Then MoveEntity(d\obj2, Sin(d\openstate)* (d\fastopen+1) * FPSfactor / 80.0, 0, 0)		
						Case 1
							d\openstate = Min(180, d\openstate + FPSfactor * 0.8)
							MoveEntity(d\obj, Sin(d\openstate) * FPSfactor / 180.0, 0, 0)
							If d\obj2 <> 0 Then MoveEntity(d\obj2, -Sin(d\openstate) * FPSfactor / 180.0, 0, 0)	
						Case 2
							d\openstate = Min(180, d\openstate + FPSfactor * 2 * (d\fastopen+1))
							MoveEntity(d\obj, Sin(d\openstate) * (d\fastopen+1) * FPSfactor / 85.0, 0, 0)
							If d\obj2 <> 0 Then MoveEntity(d\obj2, Sin(d\openstate)* (d\fastopen*2+1) * FPSfactor / 120.0, 0, 0)		
					End Select
				Else
					d\fastopen = 0
					ResetEntity(d\obj)
					If d\obj2 <> 0 Then ResetEntity(d\obj2)
					If d\timerstate > 0 Then
						d\timerstate = Max(0, d\timerstate - FPSfactor)
						If d\timerstate + FPSfactor > 110 And d\timerstate <= 110 Then PlaySound2(CautionSFX, Camera, d\obj)
						If d\timerstate = 0 Then d\open = (Not d\open) : PlaySound2(CloseDoorSFX(Min(d\dir,1),Rand(0, 2)), Camera, d\obj)
					EndIf
					If d\AutoClose And RemoteDoorOn = True Then
						If EntityDistance(Camera, d\obj) < 2.1 Then
							If (Not Wearing714) Then PlaySound_Strict HorrorSFX(7)
							d\open = False : PlaySound2(CloseDoorSFX(Min(d\dir,1), Rand(0, 2)), Camera, d\obj) : d\AutoClose = False
						EndIf
					End If				
				End If
			Else
				If d\openstate > 0 Then
					Select d\dir
						Case 0
							d\openstate = Max(0, d\openstate - FPSfactor * 2 * (d\fastopen+1))
							MoveEntity(d\obj, Sin(d\openstate) * -FPSfactor * (d\fastopen+1) / 80.0, 0, 0)
							If d\obj2 <> 0 Then MoveEntity(d\obj2, Sin(d\openstate) * (d\fastopen+1) * -FPSfactor / 80.0, 0, 0)	
						Case 1
							d\openstate = Max(0, d\openstate - FPSfactor*0.8)
							MoveEntity(d\obj, Sin(d\openstate) * -FPSfactor / 180.0, 0, 0)
							If d\obj2 <> 0 Then MoveEntity(d\obj2, Sin(d\openstate) * FPSfactor / 180.0, 0, 0)
						Case 2
							d\openstate = Max(0, d\openstate - FPSfactor * 2 * (d\fastopen+1))
							MoveEntity(d\obj, Sin(d\openstate) * -FPSfactor * (d\fastopen+1) / 85.0, 0, 0)
							If d\obj2 <> 0 Then MoveEntity(d\obj2, Sin(d\openstate) * (d\fastopen+1) * -FPSfactor / 120.0, 0, 0)	
					End Select
					
					If d\angle = 0 Or d\angle=180 Then
						If Abs(EntityZ(d\frameobj, True)-EntityZ(Collider))<0.15 Then
							If Abs(EntityX(d\frameobj, True)-EntityX(Collider))<0.7*(d\dir*2+1) Then
								z# = CurveValue(EntityZ(d\frameobj,True)+0.15*Sgn(EntityZ(Collider)-EntityZ(d\frameobj, True)), EntityZ(Collider), 5)
								PositionEntity Collider, EntityX(Collider), EntityY(Collider), z
							EndIf
						EndIf
					Else
						If Abs(EntityX(d\frameobj, True)-EntityX(Collider))<0.15 Then	
							If Abs(EntityZ(d\frameobj, True)-EntityZ(Collider))<0.7*(d\dir*2+1) Then
								x# = CurveValue(EntityX(d\frameobj,True)+0.15*Sgn(EntityX(Collider)-EntityX(d\frameobj, True)), EntityX(Collider), 5)
								PositionEntity Collider, x, EntityY(Collider), EntityZ(Collider)
							EndIf
						EndIf
					EndIf
					
				Else
					d\fastopen = 0
					PositionEntity(d\obj, EntityX(d\frameobj, True), EntityY(d\frameobj, True), EntityZ(d\frameobj, True))
					If d\obj2 <> 0 Then PositionEntity(d\obj2, EntityX(d\frameobj, True), EntityY(d\frameobj, True), EntityZ(d\frameobj, True))
					If d\obj2 <> 0 And d\dir = 0 Then
						MoveEntity(d\obj, 0, 0, 8.0 * RoomScale)
						MoveEntity(d\obj2, 0, 0, 8.0 * RoomScale)
					EndIf	
				End If
			End If
			
		EndIf
		
	Next
End Function

Function UseDoor(d.Doors, showmsg%=True)
	Local temp% = 0
	If d\KeyCard > 0 Then
		If SelectedItem = Null Then
			If showmsg = True Then 
				Msg = "You need a key card to operate the door"
				MsgTimer = 70 * 5
			EndIf
			Return
		Else
			Select SelectedItem\itemtemplate\tempname
				Case "key1"
					temp = 1
				Case "key2"
					temp = 2
				Case "key3"
					temp = 3
				Case "key4"
					temp = 4
				Case "key5"
					temp = 5
				Case "key6"
					temp = 6
				Default 
					temp = -1
			End Select
			
			If temp =-1 Then 
				If showmsg = True Then 
					Msg = "You need a key card to operate the door"
					MsgTimer = 70 * 5
				EndIf
				Return				
			ElseIf temp >= d\KeyCard 
				SelectedItem = Null
				If showmsg = True Then
					If d\locked Then
						PlaySound_Strict KeyCardSFX2
						Msg = "You insert the key card into the slot but nothing happens"
						MsgTimer = 70 * 5
						Return
					Else
						PlaySound_Strict KeyCardSFX1
						Msg = "You inserted the key card into the slot"
						MsgTimer = 70 * 5		
					EndIf
				EndIf
			Else
				SelectedItem = Null
				If showmsg = True Then 
					PlaySound_Strict KeyCardSFX2					
					If d\locked Then
						Msg = "You insert the key card into the slot but nothing happens"
					Else
						Msg = "You need a key card with a higher security clearance to operate the door"
					EndIf
					MsgTimer = 70 * 5							
				EndIf
				Return
			End If
		EndIf	
	ElseIf d\KeyCard < 0
		;I can't find any way to produce short circuited boolean expressions so work around this by using a temporary variable - risingstar64
		If SelectedItem <> Null Then
			temp = (SelectedItem\itemtemplate\tempname = "hand" And d\KeyCard=-1) Or (SelectedItem\itemtemplate\tempname = "hand2" And d\KeyCard=-2)
		EndIf
		If temp <> 0 Then
			PlaySound_Strict KeyCardSFX1
			Msg = "You insert one of the fingers on the scanner"
			MsgTimer = 70 * 5
		Else
			If showmsg = True Then 
				PlaySound_Strict KeyCardSFX2	
				Msg = "''Incorrect DNA verification''"
				MsgTimer = 70 * 5
			EndIf
			Return			
		EndIf
	Else
		If d\locked Then
			If showmsg = True Then 
				If Not (d\IsElevatorDoor>0) Then
					PlaySound_Strict KeyCardSFX2
					Msg = "You push the button but nothing happens"
					MsgTimer = 70 * 5
				Else
					If d\IsElevatorDoor = 1 Then
						Msg = "You called the elevator"
						MsgTimer = 70 * 5
					ElseIf (Msg<>"You called the elevator") Or (MsgTimer<60*5) Then
						;people like spamming the elevator buttons for some reason
						;so make sure they can see the "called the elevator" message
						Msg = "The elevator is moving"
						MsgTimer = 70 * 5
					EndIf
				EndIf
				
			EndIf
			Return
		EndIf	
	EndIf
	
	d\open = (Not d\open)
	If d\LinkedDoor <> Null Then d\LinkedDoor\open = (Not d\LinkedDoor\open)
	
	Local sound = 0
	If d\dir = 1 Then sound = 0 Else sound=Rand(0, 2)
	
	If d\open Then
		If d\LinkedDoor <> Null Then d\LinkedDoor\timerstate = d\LinkedDoor\timer
		d\timerstate = d\timer
		d\SoundCHN = PlaySound2 (OpenDoorSFX(d\dir, sound), Camera, d\obj)
	Else
		d\SoundCHN = PlaySound2 (CloseDoorSFX(d\dir, sound), Camera, d\obj)
	End If
		
	
End Function

Function RemoveDoor(d.Doors)
	If d\buttons[0] <> 0 Then EntityParent d\buttons[0], 0
	If d\buttons[1] <> 0 Then EntityParent d\buttons[1], 0	
	
	If d\obj <> 0 Then FreeEntity d\obj
	If d\obj2 <> 0 Then FreeEntity d\obj2
	If d\frameobj <> 0 Then FreeEntity d\frameobj
	If d\buttons[0] <> 0 Then FreeEntity d\buttons[0]
	If d\buttons[1] <> 0 Then FreeEntity d\buttons[1]	
	
	Delete d
End Function

DrawLoading(40,True)

Include "MapSystem.bb"

DrawLoading(80,True)

Include "NPCs.bb"

;-------------------------------------  Events --------------------------------------------------------------

Type Events
	Field EventName$
	Field room.Rooms
	
	Field EventState#, EventState2#, EventState3#
	Field SoundCHN%, SoundCHN2%
	Field Sound, Sound2
	
	Field EventStr$
	
	Field img%
End Type 

Function CreateEvent.Events(eventname$, roomname$, id%, prob# = 0.0)
	;roomname = the name of the room(s) you want the event to be assigned to
	
	;the id-variable determines which of the rooms the event is assigned to,
	;0 will assign it to the first generated room, 1 to the second, etc
	
	;the prob-variable can be used to randomly assign events into some rooms
	;0.5 means that there's a 50% chance that event is assigned to the rooms
	;1.0 means that the event is assigned to every room
	;the id-variable is ignored if prob <> 0.0
	
	Local i% = 0, temp%, e.Events, e2.Events, r.Rooms
	
	If prob = 0.0 Then
		For r.Rooms = Each Rooms
			If (roomname = "" Or roomname = r\RoomTemplate\Name) Then
				temp = False
				For e2.Events = Each Events
					If e2\room = r Then temp = True : Exit
				Next
				
				i=i+1
				If i >= id And temp = False Then
					e.Events = New Events
					e\EventName = eventname					
					e\room = r
					Return e
				End If
			EndIf
		Next
	Else
		For r.Rooms = Each Rooms
			If (roomname = "" Or roomname = r\RoomTemplate\Name) Then
				temp = False
				For e2.Events = Each Events
					If e2\room = r Then temp = True : Exit
				Next
				
				If Rnd(0.0, 1.0) < prob And temp = False Then
					e.Events = New Events
					e\EventName = eventname					
					e\room = r
				End If
			EndIf
		Next		
	EndIf
	
	Return Null
End Function

Function InitEvents()
	Local e.Events
	
	CreateEvent("173", "173", 0)
	CreateEvent("alarm", "start", 0)
	
	CreateEvent("pocketdimension", "pocketdimension", 0)	
	
	;there's a 7% chance that 106 appears in the rooms named "tunnel"
	CreateEvent("tunnel106", "tunnel", 0, 0.07 + (0.1*SelectedDifficulty\aggressiveNPCs))
	
	;the chance for 173 appearing in the first lockroom is about 66%
	;there's a 30% chance that it appears in the later lockrooms
	If Rand(3)<3 Then CreateEvent("lockroom173", "lockroom", 0)
	CreateEvent("lockroom173", "lockroom", 0, 0.3 + (0.5*SelectedDifficulty\aggressiveNPCs))
	
	CreateEvent("room2trick", "room2", 0, 0.15)	
	
	CreateEvent("1048a", "room2", 0, 1.0)	
	
	CreateEvent("room2storage", "room2storage", 0)	
	
	;096 spawns in the first (and last) lockroom2
	CreateEvent("lockroom096", "lockroom2", 0)
	
	CreateEvent("endroom106", "endroom", Rand(0,1))
	
	CreateEvent("room2poffices2", "room2poffices2", 0)
	
	CreateEvent("room2fan", "room2_2", 0, 1.0)
	
	CreateEvent("room2elevator2", "room2elevator", 0)
	CreateEvent("room2elevator", "room2elevator", 0, 1)
	
	CreateEvent("room3storage", "room3storage", 0, 0)
	
	CreateEvent("tunnel2smoke", "tunnel2", 0, 0.2)
	CreateEvent("tunnel2", "tunnel2", Rand(0,2), 0)
	CreateEvent("tunnel2", "tunnel2", 0, (0.2*SelectedDifficulty\aggressiveNPCs))
	
	;173 appears in half of the "room2doors" -rooms
	CreateEvent("room2doors173", "room2doors", 0, 0.5 + (0.4*SelectedDifficulty\aggressiveNPCs))
	
	;the anomalous duck in room2offices2-rooms
	CreateEvent("room2offices2", "room2offices2", 0, 0.7)
	
	CreateEvent("room2closets", "room2closets", 0)	
	
	CreateEvent("room2cafeteria", "room2cafeteria", 0)	
	
	CreateEvent("room3pitduck", "room3pit", 0)
	CreateEvent("room3pit1048", "room3pit", 1)
	
	;the event that causes the door to open by itself in room2offices3
	CreateEvent("room2offices3", "room2offices3", 0, 1.0)	
	
	CreateEvent("room2servers", "room2servers", 0)	
	
	CreateEvent("room3servers", "room3servers", 0)	
	CreateEvent("room3servers", "room3servers2", 0)
	
	;the dead guard
	CreateEvent("room3tunnel","room3tunnel", 0, 0.08)
	
	CreateEvent("room4","room4", 0)
	
	If Rand(5)<5 Then 
		Select Rand(3)
			Case 1
				CreateEvent("682roar", "tunnel", Rand(0,2), 0)	
			Case 2
				CreateEvent("682roar", "room3pit", Rand(0,2), 0)		
			Case 3
				CreateEvent("682roar", "room2offices", 0, 0)	
		End Select 
	EndIf 
	
	CreateEvent("testroom173", "room2testroom2", 0, 1.0)	
	
	CreateEvent("room2tesla", "room2tesla", 0, 0.9)
	
	e = CreateEvent("room2nuke", "room2nuke", 0, 0)	
	If e <> Null Then e\EventState = 1
	
	If Rand(5) < 5 Then 
		CreateEvent("coffin106", "coffin", 0, 0)
	Else
		CreateEvent("coffin", "coffin", 0, 0)
	EndIf 
	
	CreateEvent("checkpoint", "checkpoint1", 0, 1.0)
	CreateEvent("checkpoint", "checkpoint2", 0, 1.0)
	
	CreateEvent("room3door", "room3", 0, 0.1)
	CreateEvent("room3door", "room3tunnel", 0, 0.1)	
	
	If Rand(2)=1 Then
		CreateEvent("106victim", "room3", Rand(1,2))
		CreateEvent("106sinkhole", "room3_2", Rand(2,3))
	Else
		CreateEvent("106victim", "room3_2", Rand(1,2))
		CreateEvent("106sinkhole", "room3", Rand(2,3))
	EndIf
	CreateEvent("106sinkhole", "room4", Rand(1,2))
	
	CreateEvent("room079", "room079", 0, 0)	
	
	CreateEvent("room049", "room049", 0, 0)
	
	CreateEvent("room012", "room012", 0, 0)
	
	CreateEvent("room035", "room035", 0, 0)
	
	CreateEvent("008", "008", 0, 0)
	
	e.Events = CreateEvent("room106", "room106", 0, 0)	
	If e <> Null Then e\EventState2 = 1
	
	CreateEvent("pj", "roompj", 0, 0)
	
	CreateEvent("914", "914", 0, 0)
	
	CreateEvent("toiletguard", "room2toilets", 1)
	CreateEvent("buttghost", "room2toilets", 0, 0.8)
	
	CreateEvent("room2pipes106", "room2pipes", Rand(0, 3)) 
	
	CreateEvent("room2pit", "room2pit", 0, 0.4 + (0.4*SelectedDifficulty\aggressiveNPCs))
	
	CreateEvent("testroom", "testroom", 0)
	
	CreateEvent("room2tunnel", "room2tunnel", 0)
	
	CreateEvent("room2ccont", "room2ccont", 0)
	
	CreateEvent("gateaentrance", "gateaentrance", 0)
	CreateEvent("gatea", "gatea", 0)	
	CreateEvent("exit1", "exit1", 0)
	
	CreateEvent("room205", "room205", 0)
	
	CreateEvent("room860","room860", 0)
	
	CreateEvent("room966","room966", 0)
	
	CreateEvent("room1123", "room1123", 0, 0)
	CreateEvent("room2test1074","room2test1074",0)
	;CreateEvent("room038","room038",0,0)
	;CreateEvent("room009","room009",0,0)
	;CreateEvent("medibay", "medibay", 0)
	;CreateEvent("room409", "room409", 0)
	;CreateEvent("room178", "room178", 0)
	;CreateEvent("room020", "room020", 0)
	CreateEvent("room2tesla", "room2tesla_lcz", 0, 0.9)
	CreateEvent("room2tesla", "room2tesla_hcz", 0, 0.9)
	
	;New Events in SCP:CB Version 1.3 - ENDSHN
	CreateEvent("room4tunnels","room4tunnels",0)
	CreateEvent("room2gw","room2gw",0)
	CreateEvent("dimension1499","dimension1499",0)
	CreateEvent("room1162","room1162",0)
	CreateEvent("room2scps2","room2scps2",0)
	
End Function

Include "UpdateEvents.bb"

Function RemoveEvent(e.Events)
	If e\Sound<>0 Then FreeSound_Strict e\Sound
	If e\Sound2<>0 Then FreeSound_Strict e\Sound2
	If e\img<>0 Then FreeImage e\img
	Delete e
End Function

Collisions HIT_PLAYER, HIT_MAP, 2, 2
Collisions HIT_PLAYER, HIT_PLAYER, 1, 3
Collisions HIT_ITEM, HIT_MAP, 2, 2
Collisions HIT_APACHE, HIT_APACHE, 1, 2
Collisions HIT_178, HIT_MAP, 2, 2
Collisions HIT_178, HIT_178, 1, 3

DrawLoading(90, True)

;----------------------------------- meshes and textures ----------------------------------------------------------------

Global FogTexture%, Fog%
Global GasMaskTexture%, GasMaskOverlay%
Global InfectTexture%, InfectOverlay%
Global DarkTexture%, Dark%
Global Collider%, Head%

Global GlassesTexture%, GlassesOverlay%

Global FogNVTexture%
Global NVTexture%, NVOverlay%

Global TeslaTexture%

Global LightTexture%, Light%
Dim LightSpriteTex%(5)
Global DoorOBJ%, DoorFrameOBJ%

Global LeverOBJ%, LeverBaseOBJ%

Global DoorColl%
Global ButtonOBJ%, ButtonKeyOBJ%, ButtonCodeOBJ%, ButtonScannerOBJ%

Dim DecalTextures%(20)

Global Monitor%, MonitorTexture%
Global CamBaseOBJ%, CamOBJ%

Global LiquidObj%,MTFObj%,GuardObj%,ClassDObj%
Global ApacheObj%,ApacheRotorObj%

;--------------------------------------- DL_Light (for Flashlight) ------------------------------------------------------------
;The Flashlight-Thingy is a new feature in version 1.3.0 - ENDSHN
Include "DL_Lights.bb"

;---------------------------------------------------------------------------------------------------

Include "menu.bb"
MainMenuOpen = True

;---------------------------------------------------------------------------------------------------

FlushKeys()
FlushMouse()

DrawLoading(100, True)

LoopDelay = MilliSecs()

;----------------------------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------       		MAIN LOOP                 ---------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------

Repeat
	
	Cls
	
	CurTime = MilliSecs()
	ElapsedTime = (CurTime - PrevTime) / 1000.0
	PrevTime = CurTime
	FPSfactor = Max(Min(ElapsedTime * 70, 5.0), 0.2)
	FPSfactor2 = FPSfactor
	
	If MenuOpen Or InvOpen Or OtherOpen<>Null Or ConsoleOpen Or SelectedDoor <> Null Or SelectedScreen <> Null Or Using294 Then FPSfactor = 0
	
	If Framelimit > 0 Then
	    ;Framelimit
		Local WaitingTime% = (1000.0 / Framelimit) - (MilliSecs() - LoopDelay)
		Delay WaitingTime%
		
	   LoopDelay = MilliSecs()
	EndIf
	
	;Counting the fps
	If CheckFPS < MilliSecs() Then
		FPS = ElapsedLoops
		ElapsedLoops = 0
		CheckFPS = MilliSecs()+1000
	EndIf
	ElapsedLoops = ElapsedLoops + 1
	
	DoubleClick = False
	MouseHit1 = MouseHit(1)
	If MouseHit1 Then
		If MilliSecs() - LastMouseHit1 < 800 Then DoubleClick = True
		LastMouseHit1 = MilliSecs()
	EndIf
	
	Local prevmousedown1 = MouseDown1
	MouseDown1 = MouseDown(1)
	If prevmousedown1 = True And MouseDown1=False Then MouseUp1 = True Else MouseUp1 = False
	
	MouseHit2 = MouseHit(2)
	
	If (Not MouseDown1) And (Not MouseHit1) Then GrabbedEntity = 0
	
	UpdateMusic()
	
	If MainMenuOpen Then
		ShouldPlay = 11
		UpdateMainMenu()
	Else
		ShouldPlay = Min(PlayerZone,2)
		
		DrawHandIcon = False
		
		If FPSfactor > 0 Then UpdateSecurityCams()
		
		If KeyHit(KEY_INV) Then 
			If InvOpen Then
				ResumeSounds()
				MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
			Else
				PauseSounds()
			EndIf
			InvOpen = Not InvOpen
			If OtherOpen<>Null Then OtherOpen=Null
			SelectedItem = Null 
		EndIf
		
		If PlayerRoom\RoomTemplate\Name <> "pocketdimension" And PlayerRoom\RoomTemplate\Name <> "gatea"  Then 
			
			If Rand(1500) = 1 Then
				For i = 0 To 5
					If AmbientSFX(i,CurrAmbientSFX)<>0 Then
						If ChannelPlaying(AmbientSFXCHN)=0 Then FreeSound_Strict AmbientSFX(i,CurrAmbientSFX) : AmbientSFX(i,CurrAmbientSFX) = 0
					EndIf			
				Next
				
				PositionEntity (SoundEmitter, EntityX(Camera) + Rnd(-1.0, 1.0), 0.0, EntityZ(Camera) + Rnd(-1.0, 1.0))
				
				If Rand(3)=1 Then PlayerZone = 3
				CurrAmbientSFX = Rand(0,AmbientSFXAmount(PlayerZone)-1)
				
				Select PlayerZone
					Case 0,1,2
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\ambient\zone"+(PlayerZone+1)+"\ambient"+(CurrAmbientSFX+1)+".ogg")
					Case 3
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\ambient\general\ambient"+(CurrAmbientSFX+1)+".ogg")
					Case 4
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\ambient\pre-breach\ambient"+(CurrAmbientSFX+1)+".ogg")
					Case 5
						If AmbientSFX(PlayerZone,CurrAmbientSFX)=0 Then AmbientSFX(PlayerZone,CurrAmbientSFX)=LoadSound_Strict("SFX\ambient\forest\ambient"+(CurrAmbientSFX+1)+".ogg")
				End Select
				
				AmbientSFXCHN = PlaySound2(AmbientSFX(PlayerZone,CurrAmbientSFX), Camera, SoundEmitter)
			EndIf
			If Rand(40000) = 3 Then
				If PlayerRoom\RoomTemplate\Name <> "pocketdimension" And PlayerRoom\RoomTemplate\Name <> "room860" And PlayerRoom\RoomTemplate\Name <> "173" Then
					If FPSfactor > 0 Then LightBlink = Rnd(1.0,2.0)
					PlaySound_Strict  LoadTempSound("SFX\079_"+Rand(7,10)+".ogg")
				EndIf 
			EndIf
		EndIf
		
		If (Not MenuOpen) And (Not InvOpen) And (OtherOpen=Null) And (SelectedDoor = Null) And (ConsoleOpen = False) And (Using294 = False) And (SelectedScreen = Null) And EndingTimer=>0 Then 
			LightVolume = CurveValue(TempLightVolume, LightVolume, 50.0)
			CameraFogRange(Camera, CameraFogNear*LightVolume,CameraFogFar*LightVolume)
			CameraFogColor(Camera, 0,0,0)
			CameraFogMode Camera,1
			CameraRange(Camera, 0.05, Min(CameraFogFar*LightVolume*1.5,28))	
			
			AmbientLight Brightness, Brightness, Brightness	
			PlayerSoundVolume = CurveValue(0.0, PlayerSoundVolume, 5.0)
			
			UpdateEmitters()
			MouseLook()			
			MovePlayer()
			UpdateDoors()
			UpdateEvents()
			UpdateDecals()
			UpdateMTF()
			UpdateNPCs()
			UpdateItems()
			UpdateParticles()
			UpdateScreens()
			;DL_Update()
			UpdateRoomLights()
			
		EndIf
		
		If InfiniteStamina% Then Stamina = Min(100, Stamina + (100.0-Stamina)*0.01*FPSfactor)
		
		UpdateWorld()
		ManipulateNPCBones()
		RenderWorld2()
		
		BlurVolume = Min(CurveValue(0.0, BlurVolume, 20.0),0.95)
		If BlurTimer > 0.0 Then
			BlurVolume = Max(Min(0.95, BlurTimer / 1000.0), BlurVolume)
			BlurTimer = Max(BlurTimer - FPSfactor, 0.0)
		End If
		
		UpdateBlur(BlurVolume)
		
		;[Block]
		
		Local darkA# = 0.0
		If (Not MenuOpen)  Then
			If Sanity < 0 Then
				Sanity = Min(Sanity + FPSfactor, 0.0)
				If Sanity < (-200) Then 
					darkA = Max(Min((-Sanity - 200) / 700.0, 0.6), darkA)
					If KillTimer => 0 Then 
						HeartBeatVolume = Min(Abs(Sanity+200)/500.0,1.0)
						HeartBeatRate = Max(70 + Abs(Sanity+200)/6.0,HeartBeatRate)
					EndIf
				EndIf
			End If
			
			If EyeStuck > 0 Then 
				BlinkTimer = BLINKFREQ
				EyeStuck = Max(EyeStuck-FPSfactor,0)
				
				If EyeStuck < 9000 Then BlurTimer = Max(BlurTimer, (9000-EyeStuck)*0.5)
				If EyeStuck < 6000 Then darkA = Min(Max(darkA, (6000-EyeStuck)/5000.0),1.0)
				If EyeStuck < 9000 And EyeStuck+FPSfactor =>9000 Then 
					Msg = "Your eyes are starting to hurt"
					MsgTimer = 70*6
				EndIf
			EndIf
			
			If BlinkTimer < 0 Then
				If BlinkTimer > - 5 Then
					darkA = Max(darkA, Sin(Abs(BlinkTimer * 18.0)))
				ElseIf BlinkTimer > - 15
					darkA = 1.0
				Else
					darkA = Max(darkA, Abs(Sin(BlinkTimer * 18.0)))
				EndIf
				
				If BlinkTimer <= - 20 Then BlinkTimer = BLINKFREQ
				BlinkTimer = BlinkTimer - FPSfactor
			Else
				BlinkTimer = BlinkTimer - FPSfactor * 0.6
				If EyeIrritation > 0 Then BlinkTimer=BlinkTimer-Min(EyeIrritation / 100.0 + 1.0, 4.0) * FPSfactor * BlinkEffect
				
				darkA = Max(darkA, 0.0)
			End If
			
			EyeIrritation = Max(0, EyeIrritation - FPSfactor)
			
			If BlinkEffectTimer > 0 Then
				BlinkEffectTimer = BlinkEffectTimer - (FPSfactor/70)
			Else
				BlinkEffect = CurveValue(1.0,BlinkEffect,500)
			EndIf
			
			LightBlink = Max(LightBlink - (FPSfactor / 35.0), 0)
			If LightBlink > 0 Then darkA = Min(Max(darkA, LightBlink * Rnd(0.3, 0.8)), 1.0)
			
			If Using294 Then darkA=1.0
			
			darkA = Max((1.0-SecondaryLightOn)*0.9, darkA)
			
			If KillTimer >= 0 Then
				
			Else
				InvOpen = False
				SelectedItem = Null
				SelectedScreen = Null
				SelectedMonitor = Null
				BlurTimer = Abs(KillTimer*5)
				KillTimer=KillTimer-(FPSfactor*0.8)
				If KillTimer < - 360 Then 
					MenuOpen = True 
					If SelectedEnding <> "" Then EndingTimer = Min(KillTimer,-0.1)
				EndIf
				darkA = Max(darkA, Min(Abs(KillTimer / 400.0), 1.0))
			EndIf
			
			If FallTimer < 0 Then
				InvOpen = False
				SelectedItem = Null
				SelectedScreen = Null
				SelectedMonitor = Null
				BlurTimer = Abs(FallTimer*10)
				FallTimer=FallTimer-FPSfactor
				darkA = Max(darkA, Min(Abs(FallTimer / 400.0), 1.0))				
			EndIf
			
			If SelectedItem <> Null Then
				If SelectedItem\itemtemplate\tempname = "navigator" Or SelectedItem\itemtemplate\tempname = "nav" Then darkA = Max(darkA, 0.5)
			End If
			If SelectedScreen <> Null Then darkA = Max(darkA, 0.5)
			
			EntityAlpha(Dark, darkA)	
		EndIf
		
		If LightFlash > 0 Then
			ShowEntity Light
			EntityAlpha(Light, Max(Min(LightFlash + Rnd(-0.2, 0.2), 1.0), 0.0))
			LightFlash = Max(LightFlash - (FPSfactor / 70.0), 0)
		Else
			HideEntity Light
			;EntityAlpha(Light, LightFlash)
		End If
		
		;[End block]
		
		If KeyHit(63) Then
			If SelectedDifficulty\saveType = SAVEANYWHERE Then
				If PlayerRoom\RoomTemplate\Name = "exit1" Or PlayerRoom\RoomTemplate\Name = "173" Or PlayerRoom\RoomTemplate\Name = "gatea" Then
					Msg = "You can't save in this location"
					MsgTimer = 70 * 4
				Else
					SaveGame(SavePath + CurrSave + "\")
				EndIf
			ElseIf SelectedDifficulty\saveType = SAVEONSCREENS
				If SelectedScreen=Null And SelectedMonitor=Null Then
					Msg = "Find a lit up computer screen to save"
					MsgTimer = 70 * 4						
				Else
					SaveGame(SavePath + CurrSave + "\")
				EndIf
			Else
				Msg = "Quicksaving is disabled"
				MsgTimer = 70 * 4
			EndIf
		Else If SelectedDifficulty\saveType = SAVEONSCREENS And (SelectedScreen<>Null Or SelectedMonitor<>Null)
			If Msg<>"Game saved" Or MsgTimer<=0 Then
				Msg = "Press F5 to save"
				MsgTimer = 70*5
			EndIf
			
			If MouseHit2 Then SelectedMonitor = Null
		EndIf
		
		If KeyHit(61) Then
			If ConsoleOpen Then
				UsedConsole = True
				ResumeSounds()
				MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
			Else
				PauseSounds()
			EndIf
			
			ConsoleOpen = (Not ConsoleOpen)
			FlushKeys()
		EndIf
		
		DrawGUI()
		
		If EndingTimer < 0 Then
			If SelectedEnding <> "" Then DrawEnding()
		Else
			DrawMenu()			
		EndIf
		
		UpdateConsole()
		
		If MsgTimer > 0 Then
			Color 0,0,0
			Text((GraphicWidth / 2)+1, (GraphicHeight / 2) + 201, Msg, True) 			
			Color Min(MsgTimer / 2, 255), Min(MsgTimer / 2, 255), Min(MsgTimer / 2, 255)
			Text((GraphicWidth / 2), (GraphicHeight / 2) + 200, Msg, True) 
			MsgTimer=MsgTimer-FPSfactor2 
		End If
		
		Color 255, 255, 255
		If ShowFPS Then Text 20, 20, "FPS: " + FPS
		
		
	End If
	
	If Vsync = 0 Then
		Flip 0
	Else 
		Flip 1
	EndIf
	
Forever

;----------------------------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------
;----------------------------------------------------------------------------------------------------------------------------------------------------


Function Kill()
	If GodMode Then Return
	
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then StopChannel(BreathCHN)
	EndIf
	
	If KillTimer >= 0 Then
		KillAnim = Rand(0,1)
		PlaySound_Strict(DamageSFX(0))
		If SelectedDifficulty\permaDeath Then
			DeleteDir(SavePath + CurrSave + "\")
			LoadSaveGames()
		End If
		
		KillTimer = Min(-1, KillTimer)
		ShowEntity Head
		PositionEntity(Head, EntityX(Camera, True), EntityY(Camera, True), EntityZ(Camera, True), True)
		ResetEntity (Head)
		RotateEntity(Head, 0, EntityYaw(Camera), 0)		
	EndIf
End Function

Function DrawEnding()
	
	ShowPointer()
	
	FPSfactor = 0
	EndingTimer=EndingTimer-FPSfactor2
	
	GiveAchievement(Achv055)
	If (Not UsedConsole) GiveAchievement(AchvConsole)
		
	Local x,y,width,height, temp
	Local itt.ItemTemplates, r.Rooms
	
	Select Lower(SelectedEnding)
		Case "b2", "a1"
			ClsColor Max(255+(EndingTimer)*2.8,0), Max(255+(EndingTimer)*2.8,0), Max(255+(EndingTimer)*2.8,0)
		Default
			ClsColor 0,0,0
	End Select
	
	ShouldPlay = 66
	
	Cls
	
	If EndingTimer<-200 Then
		
		If BreathCHN <> 0 Then
			If ChannelPlaying(BreathCHN) Then StopChannel BreathCHN : Stamina = 100
		EndIf
		
		If EndingTimer <-400 Then 
			If Music(5)=0 Then Music(5) = LoadSound_Strict("SFX\Music\Blue Feather.ogg")
			ShouldPlay = 5
		EndIf
		
		If EndingScreen = 0 Then 
			EndingScreen = LoadImage_Strict("GFX\endingscreen.pt")
			
			temp = LoadSound_Strict ("SFX\Ending.ogg")
			PlaySound_Strict temp
			
			PlaySound_Strict LightSFX
		EndIf
		
		If EndingTimer > -700 Then 
			
			;-200 -> -700
			;Max(50 - (Abs(KillTimer)-200),0)    =    0->50
			If Rand(1,150)<Min((Abs(EndingTimer)-200),155) Then
				DrawImage EndingScreen, GraphicWidth/2-400, GraphicHeight/2-400
			Else
				Color 0,0,0
				Rect 100,100,GraphicWidth-200,GraphicHeight-200
				Color 255,255,255
			EndIf
			
			If EndingTimer+FPSfactor2 > -450 And EndingTimer <= -450 Then
				PlaySound_Strict LoadTempSound("SFX\Ending"+SelectedEnding+".ogg")
			EndIf			
			
		Else
			
			DrawImage EndingScreen, GraphicWidth/2-400, GraphicHeight/2-400
			
			If EndingTimer < -1000 Then 
				
				width = ImageWidth(PauseMenuIMG)
				height = ImageHeight(PauseMenuIMG)
				x = GraphicWidth / 2 - width / 2
				y = GraphicHeight / 2 - height / 2
				
				DrawImage PauseMenuIMG, x, y
				
				Color(255, 255, 255)
				SetFont Font2
				Text(x + width / 2 + 40*MenuScale, y + 20*MenuScale, "THE END", True)
				SetFont Font1
				
				If AchievementsMenu=0 Then 
					x = x+132*MenuScale
					y = y+122*MenuScale
					
					Local roomamount = 0, roomsfound = 0
					For r.Rooms = Each Rooms
						roomamount = roomamount + 1
						roomsfound = roomsfound + r\found
					Next
					
					Local docamount=0, docsfound=0
					For itt.ItemTemplates = Each ItemTemplates
						If itt\tempname = "paper" Then
							docamount=docamount+1
							docsfound=docsfound+itt\found
						EndIf
					Next
					
					Local scpsEncountered=1
					For i = 0 To 24
						scpsEncountered = scpsEncountered+Achievements(i)
					Next
					
					Local achievementsUnlocked =0
					For i = 0 To MAXACHIEVEMENTS-1
						achievementsUnlocked = achievementsUnlocked + Achievements(i)
					Next
					
					Text x, y, "SCPs encountered: " +scpsEncountered
					Text x, y+20*MenuScale, "Achievements unlocked: " + achievementsUnlocked+"/"+(MAXACHIEVEMENTS-1)
					Text x, y+40*MenuScale, "Rooms found: " + roomsfound+"/"+roomamount
					Text x, y+60*MenuScale, "Documents discovered: " +docsfound+"/"+docamount
					Text x, y+80*MenuScale, "Items refined in SCP-914: " +RefinedItems			
					
					x = GraphicWidth / 2 - width / 2
					y = GraphicHeight / 2 - height / 2
					x = x+width/2
					y = y+height-100*MenuScale
					
					If DrawButton(x-145*MenuScale,y-200*MenuScale,390*MenuScale,60*MenuScale,"ACHIEVEMENTS", True) Then
						AchievementsMenu = 1
					EndIf
					
					If DrawButton(x-145*MenuScale,y-100*MenuScale,390*MenuScale,60*MenuScale,"MAIN MENU", True) Then
						PlaySound_Strict LoadTempSound("SFX\breath.ogg")
						NullGame()
						MenuOpen = False
						MainMenuOpen = True
						MainMenuTab = 0
						CurrSave = ""
						FlushKeys()
					EndIf					
				Else
					DrawMenu()
				EndIf
				
			EndIf
			
		EndIf
		
	EndIf
	
	If Fullscreen Then DrawImage CursorIMG, MouseX(),MouseY()
	
	SetFont Font1
End Function

;--------------------------------------- player controls -------------------------------------------

Function MovePlayer()
	Local Sprint# = 1.0, Speed# = 0.018, i%, angle#
	
	If SuperMan Then
		Speed = Speed * 3
		
		SuperManTimer=SuperManTimer+FPSfactor
		
		CameraShake = Sin(SuperManTimer / 5.0) * (SuperManTimer / 1500.0)
		
		If SuperManTimer > 70 * 50 Then
			DeathMSG = "A Class D jumpsuit found in [DATA REDACTED]. Upon further examination, the jumpsuit was found to be filled with 12.5 kilograms of blue ash-like substance. "
			DeathMSG = DeathMSG + "Chemical analysis of the substance remains non-conclusive. Most likely related to SCP-914."
			Kill()
			ShowEntity Fog
		Else
			BlurTimer = 500		
			HideEntity Fog
		EndIf
	End If
	
	If DeathTimer > 0 Then
		DeathTimer=DeathTimer-FPSfactor
		If DeathTimer < 1 Then DeathTimer = -1.0
	ElseIf DeathTimer < 0 
		Kill()
	EndIf
	
	Stamina = Min(Stamina + 0.15 * FPSfactor, 100.0)
	
	If StaminaEffectTimer > 0 Then
		StaminaEffectTimer = StaminaEffectTimer - (FPSfactor/70)
	Else
		StaminaEffect = CurveValue(1.0, StaminaEffect, 50)
	EndIf
	
	If PlayerRoom\RoomTemplate\Name<>"pocketdimension" Then 
		If KeyDown(KEY_SPRINT) Then
			If Stamina < 5 Then
				If ChannelPlaying(BreathCHN)=False Then BreathCHN = PlaySound_Strict(BreathSFX((WearingGasMask>0), 0))
			ElseIf Stamina < 50
				If BreathCHN=0 Then
					BreathCHN = PlaySound_Strict(BreathSFX((WearingGasMask>0), Rand(1,3)))
					ChannelVolume BreathCHN, Min((70.0-Stamina)/70.0,1.0)
				Else
					If ChannelPlaying(BreathCHN)=False Then
						BreathCHN = PlaySound_Strict(BreathSFX((WearingGasMask>0), Rand(1,3)))
						ChannelVolume BreathCHN, Min((70.0-Stamina)/70.0,1.0)				
					EndIf
				EndIf
			EndIf
		EndIf
	EndIf
	
	For i = 0 To MaxItemAmount-1
		If Inventory(i)<>Null Then
			If Inventory(i)\itemtemplate\tempname = "finevest" Then Stamina = Min(Stamina, 60)
		EndIf
	Next
	
	If Wearing714 Then 
		Stamina = Min(Stamina, 10)
		Sanity = Max(-850, Sanity)
	EndIf
	
	If Abs(CrouchState-Crouch)<0.001 Then 
		CrouchState = Crouch
	Else
		CrouchState = CurveValue(Crouch, CrouchState, 10.0)
	EndIf
	
	If (Not NoClip) Then 
		If ((KeyDown(KEY_DOWN) Xor KeyDown(KEY_UP)) Or (KeyDown(KEY_RIGHT) Xor KeyDown(KEY_LEFT)) And Playable) Or ForceMove>0 Then
			
			If Crouch = 0 And (KeyDown(KEY_SPRINT)) And Stamina > 0.0 Then
				Sprint = 2.5
				Stamina = Stamina - FPSfactor * 0.5 * StaminaEffect
				If Stamina <= 0 Then Stamina = -20.0
			End If
			
			If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then 
				If EntityY(Collider)<2000*RoomScale Or EntityY(Collider)>2608*RoomScale Then
					Stamina = 0
					Speed = 0.015
					Sprint = 1.0					
				EndIf
			EndIf	
			
			If ForceMove>0 Then Speed=Speed*ForceMove
			
			If SelectedItem<>Null Then
				If SelectedItem\itemtemplate\tempname = "firstaid" Or SelectedItem\itemtemplate\tempname = "finefirstaid" Or SelectedItem\itemtemplate\tempname = "firstaid2" Then 
					Sprint = 0
				EndIf
			EndIf
			
			Local temp# = (Shake Mod 360), tempchn%
			Shake# = (Shake + FPSfactor * Min(Sprint, 1.5) * 7) Mod 720
			If temp < 180 And (Shake Mod 360) >= 180 Then
				If CurrStepSFX=0 Then
					temp = GetStepSound()
					
					If Sprint = 1.0 Then
						PlayerSoundVolume = Max(4.0,PlayerSoundVolume)
						tempchn% = PlaySound_Strict(StepSFX(temp, 1, Rand(0, 3)))
						ChannelVolume tempchn, 1.0-(Crouch*0.6)
					Else
						PlayerSoundVolume = Max(2.5-(Crouch*0.6),PlayerSoundVolume)
						tempchn% = PlaySound_Strict(StepSFX(temp, 0, Rand(0, 3)))
						ChannelVolume tempchn, 1.0-(Crouch*0.6)
					End If
				ElseIf CurrStepSFX=1
					tempchn% = PlaySound_Strict(Step2SFX(Rand(0, 2)))
					ChannelVolume tempchn, 1.0-(Crouch*0.4)
				ElseIf CurrStepSFX=2
					tempchn% = PlaySound_Strict(Step2SFX(Rand(3,5)))
					ChannelVolume tempchn, 1.0-(Crouch*0.4)
				EndIf
				
			EndIf	
		EndIf
	Else ;noclip on
		If (KeyDown(KEY_SPRINT)) Then 
			Sprint = 2.5
		ElseIf KeyDown(KEY_CROUCH)
			Sprint = 0.5
		EndIf
	EndIf
	
	If KeyHit(KEY_CROUCH) And Playable Then Crouch = (Not Crouch)
	
	Local temp2# = (Speed * Sprint) / (1.0+CrouchState)
	
	If NoClip Then 
		Shake = 0
		CurrSpeed = 0
		CrouchState = 0
		Crouch = 0
		
		RotateEntity Collider, WrapAngle(EntityPitch(Camera)), WrapAngle(EntityYaw(Camera)), 0
		
		temp2 = temp2 * NoClipSpeed
		
		If KeyDown(KEY_DOWN) Then MoveEntity Collider, 0, 0, -temp2*FPSfactor
		If KeyDown(KEY_UP) Then MoveEntity Collider, 0, 0, temp2*FPSfactor
		
		If KeyDown(KEY_LEFT) Then MoveEntity Collider, -temp2*FPSfactor, 0, 0
		If KeyDown(KEY_RIGHT) Then MoveEntity Collider, temp2*FPSfactor, 0, 0	
		
		ResetEntity Collider
	Else
		temp2# = temp2 / Max((Injuries+3.0)/3.0,1.0)
		If Injuries > 0.5 Then 
			temp2 = temp2*Min((Sin(Shake/2)+1.2),1.0)
		EndIf
		
		temp = False
		If KeyDown(KEY_DOWN) And Playable Then 
			temp = True 
			angle = 180
			If KeyDown(KEY_LEFT) Then angle = 135 
			If KeyDown(KEY_RIGHT) Then angle = -135 
		ElseIf (KeyDown(KEY_UP) And Playable) Then; Or ForceMove>0
			temp = True
			angle = 0
			If KeyDown(KEY_LEFT) Then angle = 45 
			If KeyDown(KEY_RIGHT) Then angle = -45 
		ElseIf ForceMove>0 Then
			temp=True
			angle = ForceAngle
		Else If Playable Then
			If KeyDown(KEY_LEFT) Then angle = 90 : temp = True
			If KeyDown(KEY_RIGHT) Then angle = -90 : temp = True 
		EndIf		
		
		angle = WrapAngle(EntityYaw(Collider,True)+angle+90.0)
		
		If temp Then 
			CurrSpeed = CurveValue(temp2, CurrSpeed, 20.0)
		Else
			CurrSpeed = Max(CurveValue(0.0, CurrSpeed-0.1, 1.0),0.0)
		EndIf
		
		TranslateEntity Collider, Cos(angle)*CurrSpeed * FPSfactor, 0, Sin(angle)*CurrSpeed * FPSfactor, True
		
		Local CollidedFloor% = False
		For i = 1 To CountCollisions(Collider)
			If CollisionY(Collider, i) < EntityY(Collider) - 0.25 Then CollidedFloor = True
		Next
		
		If CollidedFloor = True Then
			If DropSpeed# < - 0.07 Then 
				If CurrStepSFX=0 Then
					PlaySound_Strict(StepSFX(GetStepSound(), 0, Rand(0, 3)))					
				ElseIf CurrStepSFX=1
					PlaySound_Strict(Step2SFX(Rand(0, 2)))
				ElseIf CurrStepSFX=2
					PlaySound_Strict(Step2SFX(Rand(3, 5)))
				EndIf
				PlayerSoundVolume = Max(3.0,PlayerSoundVolume)
			EndIf
			DropSpeed# = 0
		Else
			DropSpeed# = Min(Max(DropSpeed - 0.006 * FPSfactor, -2.0), 0.0)
		EndIf	
		
		TranslateEntity Collider, 0, DropSpeed * FPSfactor, 0
	EndIf
	
	ForceMove = False
	
	If Injuries > 1.0 Then
		temp2 = Bloodloss
		BlurTimer = Max(Max(Sin(MilliSecs()/100.0)*Bloodloss*30.0,Bloodloss*2*(2.0-CrouchState)),BlurTimer)
		Bloodloss = Min(Bloodloss + (Min(Injuries,3.5)/300.0)*FPSfactor,100)
		
		If temp2 <= 60 And Bloodloss > 60 Then
			Msg = "You are feeling weak from the blood loss"
			MsgTimer = 70*4
		EndIf
	EndIf
	
	UpdateInfect()
	
	If Bloodloss > 0 Then
		If Rnd(200)<Min(Injuries,4.0) Then
			pvt = CreatePivot()
			PositionEntity pvt, EntityX(Collider)+Rnd(-0.05,0.05),EntityY(Collider)-0.05,EntityZ(Collider)+Rnd(-0.05,0.05)
			TurnEntity pvt, 90, 0, 0
			EntityPick(pvt,0.3)
			de.decals = CreateDecal(Rand(15,16), PickedX(), PickedY()+0.005, PickedZ(), 90, Rand(360), 0)
			de\size = Rnd(0.03,0.08)*Min(Injuries,3.0) : EntityAlpha(de\obj, 1.0) : ScaleSprite de\obj, de\size, de\size
			tempchn% = PlaySound_Strict (DripSFX(Rand(0,2)))
			ChannelVolume tempchn, Rnd(0.0,0.8)
			ChannelPitch tempchn, Rand(20000,30000)
			
			FreeEntity pvt
		EndIf
		
		CurrCameraZoom = Max(CurrCameraZoom, (Sin(Float(MilliSecs())/20.0)+1.0)*Bloodloss*0.2)
		
		If Bloodloss > 60 Then Crouch = True
		If Bloodloss => 100 Then 
			Kill()
			HeartBeatVolume = 0.0
		ElseIf Bloodloss > 80.0
			HeartBeatRate = Max(150-(Bloodloss-80)*5,HeartBeatRate)
			HeartBeatVolume = Max(HeartBeatVolume, 0.75+(Bloodloss-80.0)*0.0125)	
		ElseIf Bloodloss > 35.0
			HeartBeatRate = Max(70+Bloodloss,HeartBeatRate)
			HeartBeatVolume = Max(HeartBeatVolume, (Bloodloss-35.0)/60.0)			
		EndIf
	EndIf
	
	If Playable Then
		If KeyHit(KEY_BLINK) Then BlinkTimer = 0
		If KeyDown(KEY_BLINK) And BlinkTimer < - 10 Then BlinkTimer = -10		
	EndIf
	
	
	If HeartBeatVolume > 0 Then
		If HeartBeatTimer <= 0 Then
			tempchn = PlaySound_Strict (HeartBeatSFX)
			ChannelVolume tempchn, HeartBeatVolume
			
			HeartBeatTimer = 70.0*(60.0/Max(HeartBeatRate,1.0))
		Else
			HeartBeatTimer = HeartBeatTimer - FPSfactor
		EndIf
		
		HeartBeatVolume = Max(HeartBeatVolume - FPSfactor*0.05, 0)
	EndIf
	
End Function

Function MouseLook()
	Local i%
	
	CameraShake = Max(CameraShake - (FPSfactor / 10), 0)
	
	;CameraZoomTemp = CurveValue(CurrCameraZoom,CameraZoomTemp, 5.0)
	CameraZoom(Camera, Min(1.0+(CurrCameraZoom/400.0),1.1))
	CurrCameraZoom = Max(CurrCameraZoom - FPSfactor, 0)
	
	If KillTimer >= 0 And FallTimer >=0 Then
		
		HeadDropSpeed = 0
		
		;If 0 Then 
		;fixing the black screen bug with some bubblegum code 
		Local Zero# = 0.0
		Local Nan1# = 0.0 / Zero
		If Int(EntityX(Collider))=Int(Nan1) Then
			
			PositionEntity Collider, EntityX(Camera, True), EntityY(Camera, True) - 0.5, EntityZ(Camera, True), True
			Msg = "EntityX(Collider) = NaN, RESETTING COORDINATES    -    New coordinates: "+EntityX(Collider)
			MsgTimer = 300				
		EndIf
		;EndIf
		
		Local up# = (Sin(Shake) / (20.0+CrouchState*20.0))*0.6;, side# = Cos(Shake / 2.0) / 35.0		
		Local roll# = Max(Min(Sin(Shake/2)*2.5*Min(Injuries+0.25,3.0),8.0),-8.0)
		
		;k‰‰nnet‰‰n kameraa sivulle jos pelaaja on vammautunut
		;RotateEntity Collider, EntityPitch(Collider), EntityYaw(Collider), Max(Min(up*30*Injuries,50),-50)
		PositionEntity Camera, EntityX(Collider), EntityY(Collider), EntityZ(Collider)
		RotateEntity Camera, 0, EntityYaw(Collider), roll*0.5
		
		MoveEntity Camera, side, up + 0.6 + CrouchState * -0.3, 0
		
		;RotateEntity Collider, EntityPitch(Collider), EntityYaw(Collider), 0
		;moveentity player, side, up, 0	
		; -- Update the smoothing que To smooth the movement of the mouse.
		mouse_x_speed_1# = CurveValue(MouseXSpeed() * (MouseSens + 0.6) , mouse_x_speed_1, 6.0 / (MouseSens + 1.0)) 
		If Int(mouse_x_speed_1) = Int(Nan1) Then mouse_x_speed_1 = 0
		
		If InvertMouse Then
			mouse_y_speed_1# = CurveValue(-MouseYSpeed() * (MouseSens + 0.6), mouse_y_speed_1, 6.0/(MouseSens+1.0)) 
		Else
			mouse_y_speed_1# = CurveValue(MouseYSpeed () * (MouseSens + 0.6), mouse_y_speed_1, 6.0/(MouseSens+1.0)) 
		EndIf
		If Int(mouse_y_speed_1) = Int(Nan1) Then mouse_y_speed_1 = 0
		
		Local the_yaw# = ((mouse_x_speed_1#)) * mouselook_x_inc# / (1.0+WearingVest)
		Local the_pitch# = ((mouse_y_speed_1#)) * mouselook_y_inc# / (1.0+WearingVest)
		
		TurnEntity Collider, 0.0, -the_yaw#, 0.0 ; Turn the user on the Y (yaw) axis.
		user_camera_pitch# = user_camera_pitch# + the_pitch#
		; -- Limit the user;s camera To within 180 degrees of pitch rotation. ;EntityPitch(); returns useless values so we need To use a variable To keep track of the camera pitch.
		If user_camera_pitch# > 70.0 Then user_camera_pitch# = 70.0
		If user_camera_pitch# < - 70.0 Then user_camera_pitch# = -70.0
		
		RotateEntity Camera, WrapAngle(user_camera_pitch + Rnd(-CameraShake, CameraShake)), WrapAngle(EntityYaw(Collider) + Rnd(-CameraShake, CameraShake)), roll ; Pitch the user;s camera up And down.
		
		If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then
			If EntityY(Collider)<2000*RoomScale Or EntityY(Collider)>2608*RoomScale Then
				RotateEntity Camera, WrapAngle(EntityPitch(Camera)),WrapAngle(EntityYaw(Camera)), roll+WrapAngle(Sin(MilliSecs()/150.0)*30.0) ; Pitch the user;s camera up And down.
			EndIf
		EndIf
		
	Else
		HideEntity Collider
		PositionEntity Camera, EntityX(Head), EntityY(Head), EntityZ(Head)
		
		Local CollidedFloor% = False
		For i = 1 To CountCollisions(Head)
			If CollisionY(Head, i) < EntityY(Head) - 0.01 Then CollidedFloor = True
		Next
		
		If CollidedFloor = True Then
			HeadDropSpeed# = 0
		Else
			
			If KillAnim = 0 Then 
				MoveEntity Head, 0, 0, HeadDropSpeed
				RotateEntity(Head, CurveAngle(-90.0, EntityPitch(Head), 20.0), EntityYaw(Head), EntityRoll(Head))
				RotateEntity(Camera, CurveAngle(EntityPitch(Head) - 40.0, EntityPitch(Camera), 40.0), EntityYaw(Camera), EntityRoll(Camera))
			Else
				MoveEntity Head, 0, 0, -HeadDropSpeed
				RotateEntity(Head, CurveAngle(90.0, EntityPitch(Head), 20.0), EntityYaw(Head), EntityRoll(Head))
				RotateEntity(Camera, CurveAngle(EntityPitch(Head) + 40.0, EntityPitch(Camera), 40.0), EntityYaw(Camera), EntityRoll(Camera))
			EndIf
			
			HeadDropSpeed# = HeadDropSpeed - 0.002 * FPSfactor
		EndIf
		
		If InvertMouse Then
			TurnEntity (Camera, -MouseYSpeed() * 0.05 * FPSfactor, -MouseXSpeed() * 0.15 * FPSfactor, 0)
		Else
			TurnEntity (Camera, MouseYSpeed() * 0.05 * FPSfactor, -MouseXSpeed() * 0.15 * FPSfactor, 0)
		End If
		
	EndIf
	
	;pˆlyhiukkasia
	If Rand(35) = 1 Then
		Local pvt% = CreatePivot()
		PositionEntity(pvt, EntityX(Camera, True), EntityY(Camera, True), EntityZ(Camera, True))
		RotateEntity(pvt, 0, Rnd(360), 0)
		If Rand(2) = 1 Then
			MoveEntity(pvt, 0, Rnd(-0.5, 0.5), Rnd(0.5, 1.0))
		Else
			MoveEntity(pvt, 0, Rnd(-0.5, 0.5), Rnd(0.5, 1.0))
		End If
		
		Local p.Particles = CreateParticle(EntityX(pvt), EntityY(pvt), EntityZ(pvt), 2, 0.002, 0, 300)
		p\speed = 0.001
		RotateEntity(p\pvt, Rnd(-20, 20), Rnd(360), 0)
		
		p\SizeChange = -0.00001
		
		FreeEntity pvt
	End If
	
	; -- Limit the mouse;s movement. Using this method produces smoother mouselook movement than centering the mouse Each loop.
	If (MouseX() > mouse_right_limit) Or (MouseX() < mouse_left_limit) Or (MouseY() > mouse_bottom_limit) Or (MouseY() < mouse_top_limit)
		MoveMouse viewport_center_x, viewport_center_y
	EndIf
	
	If WearingGasMask Or WearingHazmat Then
		If WearingGasMask = 2 Then Stamina = Min(100, Stamina + (100.0-Stamina)*0.01*FPSfactor)
		If WearingHazmat = 2 Then 
			Stamina = Min(100, Stamina + (100.0-Stamina)*0.01*FPSfactor)
		ElseIf WearingHazmat=1
			Stamina = Min(60, Stamina)
		EndIf
		
		ShowEntity(GasMaskOverlay)
	Else
		HideEntity(GasMaskOverlay)
	End If
	
	If (Not WearingNightVision=0) Then
		;AmbientLightRooms(60)
		ShowEntity(NVOverlay)
		If WearingNightVision=2 Then
			EntityColor(NVOverlay, 0,100,255)
		Else
			EntityColor(NVOverlay, 0,255,0)
		EndIf
		EntityTexture(Fog, FogNVTexture)
	Else
		;AmbientLightRooms(0)
		HideEntity(NVOverlay)
		EntityTexture(Fog, FogTexture)
	EndIf
	
	If Wearing178>0 Then
		If Music(14)=0 Then Music(14)=LoadSound_Strict("SFX\178ambient.ogg")
		ShouldPlay = 14
		ShowEntity(GlassesOverlay)
	Else
		HideEntity(GlassesOverlay)
	EndIf
	
	canSpawn178%=0
	
	If Wearing178<>1 Then
		For n.NPCs = Each NPCs
			If (n\NPCtype = NPCtype178) Then
				If n\State3>0 Then canSpawn178=1
				If (n\State<=0) And (n\State3=0) Then
					RemoveNPC(n)
				Else If EntityDistance(Collider,n\Collider)>HideDistance*1.5 Then
					RemoveNPC(n)
				EndIf
			EndIf
		Next
	EndIf
	
	If (canSpawn178=1) Or (Wearing178=1) Then
		tempint%=0
		For n.NPCs = Each NPCs
			If (n\NPCtype = NPCtype178) Then
				tempint=tempint+1
				If EntityDistance(Collider,n\Collider)>HideDistance*1.5 Then
					RemoveNPC(n)
				EndIf
				;If n\State<=0 Then RemoveNPC(n)
			EndIf
		Next
		If tempint<10 Then ;create the npcs
			For w.WayPoints = Each WayPoints
				Local dist#
				dist=EntityDistance(Collider,w\obj)
				If (dist<HideDistance*1.5) And (dist>1.2) And (w\door = Null) And (Rand(0,1)=1) Then
					tempint2=True
					For n.NPCs = Each NPCs
						If n\NPCtype=NPCtype178 Then
							If EntityDistance(n\Collider,w\obj)<0.5
								tempint2=False
								Exit
							EndIf
						EndIf
					Next
					If tempint2 Then
						CreateNPC(NPCtype178, EntityX(w\obj,True),EntityY(w\obj,True)+0.15,EntityZ(w\obj,True))
					EndIf	
				EndIf
			Next
		EndIf
	EndIf
	
	For i = 0 To 5
		If SCP1025state[i]>0 Then
			Select i
				Case 0 ;common cold
					If FPSfactor>0 Then 
						If Rand(1000)=1 Then
							If CoughCHN = 0 Then
								CoughCHN = PlaySound_Strict(CoughSFX(Rand(0, 2)))
							Else
								If Not ChannelPlaying(CoughCHN) Then CoughCHN = PlaySound_Strict(CoughSFX(Rand(0, 2)))
							End If
						EndIf
					EndIf
					Stamina = Stamina - FPSfactor * 0.3
				Case 1 ;chicken pox
					If Rand(9000)=1 And Msg="" Then
						Msg="Your skin is feeling itchy"
						MsgTimer =70*4
					EndIf
				Case 2 ;cancer of the lungs
					If FPSfactor>0 Then 
						If Rand(800)=1 Then
							If CoughCHN = 0 Then
								CoughCHN = PlaySound_Strict(CoughSFX(Rand(0, 2)))
							Else
								If Not ChannelPlaying(CoughCHN) Then CoughCHN = PlaySound_Strict(CoughSFX(Rand(0, 2)))
							End If
						EndIf
					EndIf
					Stamina = Stamina - FPSfactor * 0.1
				Case 3 ;appendicitis
					;0.035/sec = 2.1/min
					SCP1025state[i]=SCP1025state[i]+FPSfactor*0.0005
					If SCP1025state[i]>20.0 Then
						If SCP1025state[i]-FPSfactor<=20.0 Then Msg="The pain in your stomach is getting unbearable"
						Stamina = Stamina - FPSfactor * 0.3
					ElseIf SCP1025state[i]>10.0
						If SCP1025state[i]-FPSfactor<=10.0 Then Msg="Your stomach is aching"
					EndIf
				Case 4 ;asthma
					If Stamina < 35 Then
						If Rand(Int(140+Stamina*8))=1 Then
							If CoughCHN = 0 Then
								CoughCHN = PlaySound_Strict(CoughSFX(Rand(0, 2)))
							Else
								If Not ChannelPlaying(CoughCHN) Then CoughCHN = PlaySound_Strict(CoughSFX(Rand(0, 2)))
							End If
						EndIf
						CurrSpeed = CurveValue(0, CurrSpeed, 10+Stamina*15)
					EndIf
				Case 5;cardiac arrest
					SCP1025state[i]=SCP1025state[i]+FPSfactor*0.35
					;35/sec
					If SCP1025state[i]>110 Then
						HeartBeatRate=0
						BlurTimer = Max(BlurTimer, 500)
						If SCP1025state[i]>140 Then 
							DeathMSG = "''He died of a cardiac arrest after reading SCP-1025, that's for sure. Is there such a thing as psychosomatic cardiac arrest, or does SCP-1025 have some "
							DeathMSG = DeathMSG + "anomalous properties we're not yet aware of?''"
							Kill()
						EndIf
					Else
						HeartBeatRate=Max(HeartBeatRate, 70+SCP1025state[i])
						HeartBeatVolume = 1.0
					EndIf
			End Select 
		EndIf
	Next
	
	
End Function

;--------------------------------------- GUI, menu etc ------------------------------------------------

Function DrawGUI()
	
	Local temp%, x%, y%, z%, i%, yawvalue#, pitchvalue#
	Local x2#,y2#,z2#
	Local n%, xtemp, ytemp, strtemp$
	
	Local e.Events, it.Items
	
	If MenuOpen Or SelectedDoor <> Null Or InvOpen Or OtherOpen<>Null Or EndingTimer < 0 Then
		ShowPointer()
	Else
		HidePointer()
	EndIf 	
	
	If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then
		For e.Events = Each Events
			If e\room = PlayerRoom And e\EventState > 600 Then
				If BlinkTimer < -3 And BlinkTimer > -11 Then
					If e\img = 0 Then
						If BlinkTimer > -5 And Rand(30)=1 Then
							If Rand(5)<5 Then PlaySound_Strict DripSFX(0)
							If e\img = 0 Then e\img = LoadImage_Strict("GFX\npcs\106face.jpg")
						EndIf
					Else
						DrawImage e\img, GraphicWidth/2-Rand(390,310), GraphicHeight/2-Rand(290,310)
					EndIf
				Else
					If e\img <> 0 Then FreeImage e\img : e\img = 0
				EndIf
					
				Exit
			EndIf
		Next
	EndIf
	
	
	If ClosestButton <> 0 And SelectedDoor = Null And InvOpen = False And OtherOpen = Null Then
		temp% = CreatePivot()
		PositionEntity temp, EntityX(Camera), EntityY(Camera), EntityZ(Camera)
		PointEntity temp, ClosestButton
		yawvalue# = WrapAngle(EntityYaw(Camera) - EntityYaw(temp))
		If yawvalue > 90 And yawvalue <= 180 Then yawvalue = 90
		If yawvalue > 180 And yawvalue < 270 Then yawvalue = 270
		pitchvalue# = WrapAngle(EntityPitch(Camera) - EntityPitch(temp))
		If pitchvalue > 90 And pitchvalue <= 180 Then pitchvalue = 90
		If pitchvalue > 180 And pitchvalue < 270 Then pitchvalue = 270
		
		FreeEntity (temp)
		
		DrawImage(HandIcon, GraphicWidth / 2 + Sin(yawvalue) * (GraphicWidth / 3) - 32, GraphicHeight / 2 - Sin(pitchvalue) * (GraphicHeight / 3) - 32)
		
		If MouseUp1 Then
			MouseUp1 = False
			If ClosestDoor <> Null Then 
				If ClosestDoor\Code <> "" Then
					SelectedDoor = ClosestDoor
				ElseIf Playable Then
					PlaySound2(ButtonSFX, Camera, ClosestButton)
					UseDoor(ClosestDoor,True)				
				EndIf
			EndIf
		EndIf
	EndIf
	
	If SelectedScreen <> Null Then
		DrawImage SelectedScreen\img, GraphicWidth/2-ImageWidth(SelectedScreen\img)/2,GraphicHeight/2-ImageHeight(SelectedScreen\img)/2
		
		If MouseUp1 Or MouseHit2 Then
			FreeImage SelectedScreen\img : SelectedScreen\img = 0
			SelectedScreen = Null
			MouseUp1 = False
		EndIf
	EndIf
	
	If ClosestItem <> Null Then
		yawvalue# = -DeltaYaw(Camera, ClosestItem\obj)
		If yawvalue > 90 And yawvalue <= 180 Then yawvalue = 90
		If yawvalue > 180 And yawvalue < 270 Then yawvalue = 270
		pitchvalue# = -DeltaPitch(Camera, ClosestItem\obj)
		If pitchvalue > 90 And pitchvalue <= 180 Then pitchvalue = 90
		If pitchvalue > 180 And pitchvalue < 270 Then pitchvalue = 270
		
		DrawImage(HandIcon, GraphicWidth / 2 + Sin(yawvalue) * (GraphicWidth / 3) - 32, GraphicHeight / 2 - Sin(pitchvalue) * (GraphicHeight / 3) - 32)
	EndIf
	
	If DrawHandIcon Then DrawImage(HandIcon, GraphicWidth / 2 - 32, GraphicHeight / 2 - 32)
	For i = 0 To 3
		If DrawArrowIcon(i) Then
			x = GraphicWidth / 2 - 32
			y = GraphicHeight / 2 - 32		
			Select i
				Case 0
					y = y - 64 - 5
				Case 1
					x = x + 64 + 5
				Case 2
					y = y + 64 + 5
				Case 3
					x = x - 5 - 64
			End Select
			DrawImage(HandIcon, x, y)
			Color 0, 0, 0
			Rect(x + 4, y + 4, 64 - 8, 64 - 8)
			DrawImage(ArrowIMG(i), x + 21, y + 21)
			DrawArrowIcon(i) = False
		End If
	Next
	
	If Using294 Then Use294()
	
	If HUDenabled Then 
		
		Local width% = 204, height% = 20
		x% = 80
		y% = GraphicHeight - 95
		
		Color 255, 255, 255	
		Rect (x, y, width, height, False)
		For i = 1 To Int(((width - 2) * (BlinkTimer / (BLINKFREQ))) / 10)
			DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
		Next	
		Color 0, 0, 0
		Rect(x - 50, y, 30, 30)
		
		If EyeIrritation > 0 Then
			Color 200, 0, 0
			Rect(x - 50 - 3, y - 3, 30 + 6, 30 + 6)
		End If
		
		Color 255, 255, 255
		Rect(x - 50 - 1, y - 1, 30 + 2, 30 + 2, False)
		
		DrawImage BlinkIcon, x - 50, y
		
		y = GraphicHeight - 55
		Color 255, 255, 255
		Rect (x, y, width, height, False)
		For i = 1 To Int(((width - 2) * (Stamina / 100.0)) / 10)
			DrawImage(StaminaMeterIMG, x + 3 + 10 * (i - 1), y + 3)
		Next	
		
		Color 0, 0, 0
		Rect(x - 50, y, 30, 30)
		
		Color 255, 255, 255
		Rect(x - 50 - 1, y - 1, 30 + 2, 30 + 2, False)
		If Crouch Then
			DrawImage CrouchIcon, x - 50, y
		Else
			DrawImage SprintIcon, x - 50, y
		EndIf
		
		If DebugHUD Then
			Color 255, 255, 255
			
			;Text x + 250, 50, "Zone: " + (EntityZ(Collider)/8.0)
			Text x - 50, 50, "Player Position: (" + f2s(EntityX(Collider), 3) + ", " + f2s(EntityY(Collider), 3) + ", " + f2s(EntityZ(Collider), 3) + ")"
			Text x - 50, 70, "Camera Position: (" + f2s(EntityX(Camera), 3)+ ", " + f2s(EntityY(Camera), 3) +", " + f2s(EntityZ(Camera), 3) + ")"
			Text x - 50, 100, "Player Rotation: (" + f2s(EntityPitch(Collider), 3) + ", " + f2s(EntityYaw(Collider), 3) + ", " + f2s(EntityRoll(Collider), 3) + ")"
			Text x - 50, 120, "Camera Rotation: (" + f2s(EntityPitch(Camera), 3)+ ", " + f2s(EntityYaw(Camera), 3) +", " + f2s(EntityRoll(Camera), 3) + ")"
			Text x - 50, 150, "Room: " + PlayerRoom\RoomTemplate\Name
			For ev.Events = Each Events
				If ev\room = PlayerRoom Then
					Text x - 50, 170, "Room event: " + ev\EventName   
					Text x - 50, 190, "state: " + ev\EventState
					Text x - 50, 210, "state2: " + ev\EventState2   
					Text x - 50, 230, "state3: " + ev\EventState3
					Exit
				EndIf
			Next
			Text x - 50, 250, "Room coordinates: (" + Floor(EntityX(PlayerRoom\obj) / 8.0 + 0.5) + ", " + Floor(EntityZ(PlayerRoom\obj) / 8.0 + 0.5) + ")"
			Text x - 50, 280, "Stamina: " + f2s(Stamina, 3)
			Text x - 50, 300, "Death timer: " + f2s(KillTimer, 3)               
			Text x - 50, 320, "Blink timer: " + f2s(BlinkTimer, 3)
			Text x - 50, 340, "Injuries: " + Injuries
			Text x - 50, 360, "Bloodloss: " + Bloodloss
			Text x - 50, 390, "SCP - 173 Position (collider): (" + f2s(EntityX(Curr173\Collider), 3) + ", " + f2s(EntityY(Curr173\Collider), 3) + ", " + f2s(EntityZ(Curr173\Collider), 3) + ")"
			Text x - 50, 410, "SCP - 173 Position (obj): (" + f2s(EntityX(Curr173\obj), 3) + ", " + f2s(EntityY(Curr173\obj), 3) + ", " + f2s(EntityZ(Curr173\obj), 3) + ")"
			;Text x - 50, 410, "SCP - 173 Idle: " + Curr173\Idle
			Text x - 50, 430, "SCP - 173 State: " + Curr173\State
			Text x - 50, 450, "SCP - 106 Position: (" + f2s(EntityX(Curr106\obj), 3) + ", " + f2s(EntityY(Curr106\obj), 3) + ", " + f2s(EntityZ(Curr106\obj), 3) + ")"
			Text x - 50, 470, "SCP - 106 Idle: " + Curr106\Idle
			Text x - 50, 490, "SCP - 106 State: " + Curr106\State
			offset% = 0
			For npc.NPCs = Each NPCs
				If npc\NPCtype = NPCtype096 Then
					Text x - 50, 510, "SCP - 096 Position: (" + f2s(EntityX(npc\obj), 3) + ", " + f2s(EntityY(npc\obj), 3) + ", " + f2s(EntityZ(npc\obj), 3) + ")"
					Text x - 50, 530, "SCP - 096 Idle: " + npc\Idle
					Text x - 50, 550, "SCP - 096 State: " + npc\State
					Text x - 50, 570, "SCP - 096 Speed: " + f2s(npc\currspeed, 5)
				EndIf
				If npc\NPCtype = NPCtypeMTF Then
					Text x - 50, 600 + 60 * offset, "MTF " + offset + " Position: (" + f2s(EntityX(npc\obj), 3) + ", " + f2s(EntityY(npc\obj), 3) + ", " + f2s(EntityZ(npc\obj), 3) + ")"
					Text x - 50, 640 + 60 * offset, "MTF " + offset + " State: " + npc\State
					Text x - 50, 620 + 60 * offset, "MTF " + offset + " LastSeen: " + npc\lastseen					
					offset = offset + 1
				EndIf
			Next
			
		EndIf
		
	EndIf
	
	Local PrevInvOpen% = InvOpen, MouseSlot% = 66
	
	Local shouldDrawHUD%=True
	If SelectedDoor <> Null Then
		If SelectedItem <> Null Then
			If SelectedItem\itemtemplate\tempname = "scp005" Then 
				shouldDrawHUD=False
				If SelectedDoor\Code<>"GEAR" Then
					SelectedDoor\locked = 0
					
					If SelectedDoor\Code = Str(AccessCode) Then
						GiveAchievement(AchvMaynard)
					ElseIf SelectedDoor\Code = "7816"
						GiveAchievement(AchvHarp)
					EndIf
					
					UseDoor(SelectedDoor,True)
					SelectedDoor = Null
					PlaySound_Strict KeyCardSFX1
					Msg = "You hold the key close to the keypad"
					MsgTimer = 70 * 5
				Else
					SelectedDoor = Null
					PlaySound_Strict KeyCardSFX2
					Msg = "You hold the key close to the keypad but nothing happens"
					MsgTimer = 70 * 5
				EndIf
			EndIf
		EndIf
		SelectedItem = Null
		
		If shouldDrawHUD Then
			pvt = CreatePivot()
			PositionEntity pvt, EntityX(ClosestButton,True),EntityY(ClosestButton,True),EntityZ(ClosestButton,True)
			RotateEntity pvt, 0, EntityYaw(ClosestButton,True)-180,0
			MoveEntity pvt, 0,0,0.22
			PositionEntity Camera, EntityX(pvt),EntityY(pvt),EntityZ(pvt)
			PointEntity Camera, ClosestButton
			FreeEntity pvt	
			
			CameraProject(Camera, EntityX(ClosestButton,True),EntityY(ClosestButton,True)+MeshHeight(ButtonOBJ)*0.015,EntityZ(ClosestButton,True))
			projY# = ProjectedY()
			CameraProject(Camera, EntityX(ClosestButton,True),EntityY(ClosestButton,True)-MeshHeight(ButtonOBJ)*0.015,EntityZ(ClosestButton,True))
			scale# = (ProjectedY()-projy)/462.0
			
			x = GraphicWidth/2-ImageWidth(KeypadHUD)*scale/2
			y = GraphicHeight/2-ImageHeight(KeypadHUD)*scale/2		
			
			SetFont Font3
			If KeypadMSG <> "" Then 
				KeypadTimer = KeypadTimer-FPSfactor2
				
				If (KeypadTimer Mod 70) < 35 Then Text GraphicWidth/2, y+124*scale, KeypadMSG, True,True
				If KeypadTimer =<0 Then
					KeypadMSG = ""
					SelectedDoor = Null
				EndIf
			Else
				Text GraphicWidth/2, y+70*scale, "ACCESS CODE: ",True,True	
				SetFont Font4
				Text GraphicWidth/2, y+124*scale, KeypadInput,True,True	
			EndIf
			
			x = x+44*scale
			y = y+249*scale
			
			For n = 0 To 3
				For i = 0 To 2
					xtemp = x+Int(58.5*scale*n)
					ytemp = y+(67*scale)*i
					
					temp = False
					If MouseOn(xtemp,ytemp, 54*scale,65*scale) And KeypadMSG = "" Then
						If MouseUp1 Then 
							PlaySound_Strict ButtonSFX
							
							Select (n+1)+(i*4)
								Case 1,2,3
									KeypadInput=KeypadInput + ((n+1)+(i*4))
								Case 4
									KeypadInput=KeypadInput + "0"
								Case 5,6,7
									KeypadInput=KeypadInput + ((n+1)+(i*4)-1)
								Case 8 ;enter
									If KeypadInput = SelectedDoor\Code Then
										PlaySound_Strict KeyCardSFX1
										
										If SelectedDoor\Code = Str(AccessCode) Then
											GiveAchievement(AchvMaynard)
										ElseIf SelectedDoor\Code = "7816"
											GiveAchievement(AchvHarp)
										EndIf									
										
										SelectedDoor\locked = 0
										UseDoor(SelectedDoor,True)
										SelectedDoor = Null
									Else
										PlaySound_Strict KeyCardSFX2
										KeypadMSG = "ACCESS DENIED"
										KeypadTimer = 210
										KeypadInput = ""	
									EndIf
								Case 9,10,11
									KeypadInput=KeypadInput + ((n+1)+(i*4)-2)
								Case 12
									KeypadInput = ""
							End Select 
							
							If Len(KeypadInput)> 4 Then KeypadInput = Left(KeypadInput,4)
						EndIf
						
					Else
						temp = False
					EndIf
					
				Next
			Next
			
			If Fullscreen Then DrawImage CursorIMG, MouseX(),MouseY()
			
			If MouseHit2 Then
				SelectedDoor = Null
				MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
			EndIf
		Else
			SelectedDoor = Null
		EndIf
	Else
		KeypadInput = ""
		KeypadTimer = 0
		KeypadMSG= ""
	EndIf
	
	If KeyHit(1) And EndingTimer = 0 Then 
		If MenuOpen Or InvOpen Then
			ResumeSounds()
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		Else
			PauseSounds()
		EndIf
		MenuOpen = (Not MenuOpen)
		
		SelectedDoor = Null
		SelectedScreen = Null
		SelectedMonitor = Null
	EndIf
	
	Local spacing%
	Local PrevOtherOpen.Items
	
	Local OtherSize%,OtherAmount%
	
	Local isEmpty%
	
	Local isMouseOn%
	
	Local closedInv%
	
	If OtherOpen<>Null Then
		;[Block]
		If (PlayerRoom\RoomTemplate\Name = "gatea") Then
			HideEntity Fog
			CameraFogRange Camera, 5,30
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 30)
		Else If (PlayerRoom\RoomTemplate\Name = "exit1") And (EntityY(Collider)>1040.0*RoomScale)
			HideEntity Fog
			CameraFogRange Camera, 5,45
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 60)
		EndIf
		
		PrevOtherOpen = OtherOpen
		OtherSize=OtherOpen\invSlots;Int(OtherOpen\state2)
		
		For i%=0 To OtherSize-1
			If OtherOpen\SecondInv[i] <> Null Then
				OtherAmount = OtherAmount+1
			EndIf
		Next
		
		;If OtherAmount > 0 Then
		;	OtherOpen\state = 1.0
		;Else
		;	OtherOpen\state = 0.0
		;EndIf
		InvOpen = False
		SelectedDoor = Null
		Local tempX% = 0
		
		width = 70
		height = 70
		spacing% = 35
		
		x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
		y = GraphicHeight / 2 - (height * OtherSize /5 + spacing * (OtherSize / 5 - 1)) / 2;height
		
		ItemAmount = 0
		For  n% = 0 To OtherSize - 1
			isMouseOn% = False
			If MouseX() > x And MouseX() < x + width Then
				If MouseY() > y And MouseY() < y + height Then
					isMouseOn = True
				EndIf
			EndIf
			
			If isMouseOn Then
				MouseSlot = n
				Color 255, 0, 0
				Rect(x - 1, y - 1, width + 2, height + 2)
			EndIf
			
			DrawFrame(x, y, width, height, (x Mod 64), (x Mod 64))
			
			If OtherOpen = Null Then Exit
			
			If OtherOpen\SecondInv[n] <> Null Then
				If (SelectedItem <> OtherOpen\SecondInv[n] Or isMouseOn) Then DrawImage(OtherOpen\SecondInv[n]\invimg, x + width / 2 - 32, y + height / 2 - 32)
			EndIf
			DebugLog "otheropen: "+(OtherOpen<>Null)
			If OtherOpen\SecondInv[n] <> Null And SelectedItem <> OtherOpen\SecondInv[n] Then
			;drawimage(OtherOpen\SecondInv[n].InvIMG, x + width / 2 - 32, y + height / 2 - 32)
				If isMouseOn Then
					Color 255, 255, 255	
					Text(x + width / 2, y + height + spacing - 15, OtherOpen\SecondInv[n]\itemtemplate\name, True)				
					If SelectedItem = Null Then
						If MouseHit1 Then
							SelectedItem = OtherOpen\SecondInv[n]
							MouseHit1 = False
							
							If DoubleClick Then
								If OtherOpen\SecondInv[n]\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(OtherOpen\SecondInv[n]\itemtemplate\sound))
								OtherOpen = Null
								closedInv=True
								InvOpen = False
								DoubleClick = False
							EndIf
							
						EndIf
					Else
						
					EndIf
				EndIf
				
				ItemAmount=ItemAmount+1
			Else
				If isMouseOn And MouseHit1 Then
					For z% = 0 To OtherSize - 1
						If OtherOpen\SecondInv[z] = SelectedItem Then OtherOpen\SecondInv[z] = Null
					Next
					OtherOpen\SecondInv[n] = SelectedItem
				EndIf
				
			EndIf					
			
			x=x+width + spacing
			tempX=tempX + 1
			If tempX = 5 Then 
				tempX=0
				y = y + height*2 
				x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
			EndIf
		Next
		
		If SelectedItem <> Null Then
			If MouseDown1 Then
				If MouseSlot = 66 Then
					DrawImage(SelectedItem\invimg, MouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, MouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				ElseIf SelectedItem <> PrevOtherOpen\SecondInv[MouseSlot]
					DrawImage(SelectedItem\invimg, MouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, MouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				EndIf
			Else
				If MouseSlot = 66 Then
					If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))
					
					ShowEntity(SelectedItem\obj)
					PositionEntity(SelectedItem\obj, EntityX(Camera), EntityY(Camera), EntityZ(Camera))
					RotateEntity(SelectedItem\obj, EntityPitch(Camera), EntityYaw(Camera), 0)
					MoveEntity(SelectedItem\obj, 0, -0.1, 0.1)
					RotateEntity(SelectedItem\obj, 0, Rand(360), 0)
					ResetEntity (SelectedItem\obj)
					;move the item so that it doesn't overlap with other items
					For it.Items = Each Items
						If it <> SelectedItem And it\Picked = False Then
							x = Abs(EntityX(SelectedItem\obj, True)-EntityX(it\obj, True))
							If x < 0.2 Then 
								z = Abs(EntityZ(SelectedItem\obj, True)-EntityZ(it\obj, True))
								If z < 0.2 Then
									While (x+z)<0.25
										MoveEntity(SelectedItem\obj, 0, 0, 0.025)
										x = Abs(EntityX(SelectedItem\obj, True)-EntityX(it\obj, True))
										z = Abs(EntityZ(SelectedItem\obj, True)-EntityZ(it\obj, True))
									Wend
								EndIf
							EndIf
						EndIf
					Next
					
					SelectedItem\DropSpeed = 0.0
					
					SelectedItem\Picked = False
					For z% = 0 To OtherSize - 1
						If OtherOpen\SecondInv[z] = SelectedItem Then OtherOpen\SecondInv[z] = Null
					Next
					
					isEmpty=True
					
					For z% = 0 To OtherSize - 1
						If OtherOpen\SecondInv[z]<>Null Then isEmpty=False : Exit
					Next
					
					If isEmpty Then
						Select OtherOpen\itemtemplate\tempname
							Case "clipboard"
								OtherOpen\invimg = OtherOpen\itemtemplate\invimg2
								SetAnimTime OtherOpen\obj,17.0
						End Select
					EndIf
					
					SelectedItem = Null
					OtherOpen = Null
					closedInv=True
					
					MoveMouse viewport_center_x, viewport_center_y
				Else
					
					If PrevOtherOpen\SecondInv[MouseSlot] = Null Then
						For z% = 0 To OtherSize - 1
							If PrevOtherOpen\SecondInv[z] = SelectedItem Then PrevOtherOpen\SecondInv[z] = Null
						Next
						PrevOtherOpen\SecondInv[MouseSlot] = SelectedItem
						SelectedItem = Null
					ElseIf PrevOtherOpen\SecondInv[MouseSlot] <> SelectedItem
						Select SelectedItem\itemtemplate\tempname
							Default
								Msg = "This item can't be used this way"
								MsgTimer = 70 * 5
						End Select					
					EndIf
					
				EndIf
				SelectedItem = Null
			EndIf
		EndIf
		
		If Fullscreen Then DrawImage CursorIMG,MouseX(),MouseY()
		If (closedInv) And (Not InvOpen) Then 
			ResumeSounds() 
			OtherOpen=Null
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		EndIf
		;[End Block]
		
	Else If InvOpen Then
		
		If (PlayerRoom\RoomTemplate\Name = "gatea") Then
			HideEntity Fog
			CameraFogRange Camera, 5,30
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 30)
		ElseIf (PlayerRoom\RoomTemplate\Name = "exit1") And (EntityY(Collider)>1040.0*RoomScale)
			HideEntity Fog
			CameraFogRange Camera, 5,45
			CameraFogColor (Camera,200,200,200)
			CameraClsColor (Camera,200,200,200)					
			CameraRange(Camera, 0.05, 60)
		EndIf
		
		SelectedDoor = Null
		
		width% = 70
		height% = 70
		spacing% = 35
		
		x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
		y = GraphicHeight / 2 - height
		
		ItemAmount = 0
		For  n% = 0 To MaxItemAmount - 1
			isMouseOn% = False
			If MouseX() > x And MouseX() < x + width Then
				If MouseY() > y And MouseY() < y + height Then
					isMouseOn = True
				End If
			EndIf
			
			If Inventory(n) <> Null Then
				Color 200, 200, 200
				Select Inventory(n)\itemtemplate\tempname 
					Case "gasmask"
						If WearingGasMask=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "supergasmask"
						If WearingGasMask=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "gasmask3"
						If WearingGasMask=3 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "hazmatsuit"
						If WearingHazmat=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "hazmatsuit2"
						If WearingHazmat=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "hazmatsuit3"
						If WearingHazmat=3 Then Rect(x - 3, y - 3, width + 6, height + 6)	
					Case "vest"
						If WearingVest=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "finevest"
						If WearingVest=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "scp714"
						If Wearing714=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
						;BoH items
					;Case "ring"
					;	If Wearing714=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
					;Case "scp178"
					;	If Wearing178=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					;Case "glasses"
					;	If Wearing178=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
						
					Case "scp178"
						If Wearing178=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "nvgoggles"
						If WearingNightVision=1 Then Rect(x - 3, y - 3, width + 6, height + 6)
					Case "supernv"
						If WearingNightVision=2 Then Rect(x - 3, y - 3, width + 6, height + 6)
				End Select
			EndIf
			
			If isMouseOn Then
				MouseSlot = n
				Color 255, 0, 0
				Rect(x - 1, y - 1, width + 2, height + 2)
			EndIf
			
			Color 255, 255, 255
			DrawFrame(x, y, width, height, (x Mod 64), (x Mod 64))
			
			If Inventory(n) <> Null Then
				If (SelectedItem <> Inventory(n) Or isMouseOn) Then 
					DrawImage(Inventory(n)\invimg, x + width / 2 - 32, y + height / 2 - 32)
				EndIf
			EndIf
			
			If Inventory(n) <> Null And SelectedItem <> Inventory(n) Then
				;drawimage(Inventory(n).InvIMG, x + width / 2 - 32, y + height / 2 - 32)
				If isMouseOn Then
					If SelectedItem = Null Then
						If MouseHit1 Then
							SelectedItem = Inventory(n)
							MouseHit1 = False
							
							If DoubleClick Then
								If Inventory(n)\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(Inventory(n)\itemtemplate\sound))
								InvOpen = False
								DoubleClick = False
							EndIf
							
						EndIf
						
						SetFont Font1
						Color 0,0,0
						Text(x + width / 2 + 1, y + height + spacing - 15 + 1, Inventory(n)\name, True)							
						Color 255, 255, 255	
						Text(x + width / 2, y + height + spacing - 15, Inventory(n)\name, True)	
						
					EndIf
				EndIf
				
				ItemAmount=ItemAmount+1
			Else
				If isMouseOn And MouseHit1 Then
					For z% = 0 To MaxItemAmount - 1
						If Inventory(z) = SelectedItem Then Inventory(z) = Null
					Next
					Inventory(n) = SelectedItem
				End If
				
			EndIf					
			
			x=x+width + spacing
			If n = 4 Then 
				y = y + height*2 
				x = GraphicWidth / 2 - (width * MaxItemAmount /2 + spacing * (MaxItemAmount / 2 - 1)) / 2
			EndIf
		Next
		
		If SelectedItem <> Null Then
			If MouseDown1 Then
				If MouseSlot = 66 Then
					DrawImage(SelectedItem\invimg, MouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, MouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				ElseIf SelectedItem <> Inventory(MouseSlot)
					DrawImage(SelectedItem\invimg, MouseX() - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, MouseY() - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				EndIf
			Else
				If MouseSlot = 66 Then
					If SelectedItem\itemtemplate\tempname <> "scp198"
						DropItem(SelectedItem)
						
						SelectedItem = Null
						InvOpen = False
						
						MoveMouse viewport_center_x, viewport_center_y
					Else
						Msg = "You can't get rid of SCP-198"
						MsgTimer = 70*6
					EndIf
				Else
					
					If Inventory(MouseSlot) = Null Then
						For z% = 0 To MaxItemAmount - 1
							If Inventory(z) = SelectedItem Then Inventory(z) = Null
						Next
						Inventory(MouseSlot) = SelectedItem
						SelectedItem = Null
					ElseIf Inventory(MouseSlot) <> SelectedItem
						Select SelectedItem\itemtemplate\tempname
							Case "paper","key1","key2","key3","key4","key5","key6","misc" ;BoH stuff
								If Inventory(MouseSlot)\itemtemplate\tempname = "clipboard" Then
									;Add an item to clipboard
									Local added.Items = Null
									If SelectedItem\itemtemplate\tempname<>"misc" Or (SelectedItem\itemtemplate\name="Playing Card" Or SelectedItem\itemtemplate\name="Mastercard") Then
										For c% = 0 To Inventory(MouseSlot)\invSlots-1
											If (Inventory(MouseSlot)\SecondInv[c] = Null)
												If SelectedItem <> Null Then
													Inventory(MouseSlot)\SecondInv[c] = SelectedItem
													Inventory(MouseSlot)\state = 1.0
													SetAnimTime Inventory(MouseSlot)\obj,0.0
													Inventory(MouseSlot)\invimg = Inventory(MouseSlot)\itemtemplate\invimg
													
													For ri% = 0 To MaxItemAmount - 1
														If Inventory(ri) = SelectedItem Then
															Inventory(ri) = Null
															PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))
														EndIf
													Next
													added = SelectedItem
													SelectedItem = Null : Exit
												EndIf
											EndIf
										Next
										If SelectedItem <> Null Then
											Msg = "This clipboard can't hold more items"
										Else
											If added\itemtemplate\tempname = "paper" Then
												Msg = "You've added this document to the clipboard"
											Else
												Msg = "You've added this "+added\itemtemplate\name+" to the clipboard"
											EndIf
											
										EndIf
										MsgTimer = 70 * 5
									Else
										Msg = "This item can't be used this way"
										MsgTimer = 70 * 5
									EndIf
								EndIf
								SelectedItem = Null
								
							Case "battery", "bat"
								Select Inventory(MouseSlot)\itemtemplate\name
									Case "S-NAV Navigator", "S-NAV 300 Navigator", "S-NAV 310 Navigator"
										If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))	
										RemoveItem (SelectedItem)
										SelectedItem = Null
										Inventory(MouseSlot)\state = 100.0
										Msg = "You replaced the battery of the navigator"
										MsgTimer = 70 * 5
									Case "S-NAV Navigator Ultimate"
										Msg = "There seems to be no place for batteries in the navigator"
										MsgTimer = 70 * 5
									Case "Radio Transceiver"
										Select Inventory(MouseSlot)\itemtemplate\tempname 
											Case "fineradio", "veryfineradio"
												Msg = "There seems to be no place for batteries in the radio"
												MsgTimer = 70 * 5
											Case "18vradio"
												Msg = "The battery doesn't seem to fit"
												MsgTimer = 70 * 5
											Case "radio"
												If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))	
												RemoveItem (SelectedItem)
												SelectedItem = Null
												Inventory(MouseSlot)\state = 100.0
												Msg = "You replaced the battery of the radio"
												MsgTimer = 70 * 5
										End Select
									Case "Night Vision Goggles"
										If Inventory(MouseSlot)\itemtemplate\tempname="nvgoggles" Then
											If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))	
											RemoveItem (SelectedItem)
											SelectedItem = Null
											Inventory(MouseSlot)\state = 1000.0
											Msg = "You replaced the battery of the goggles"
											MsgTimer = 70 * 5
										EndIf
									Default
										Msg = "This item can't be used this way"
										MsgTimer = 70 * 5	
								End Select
							Case "18vbat"
								Select Inventory(MouseSlot)\itemtemplate\name
									Case "S-NAV Navigator", "S-NAV 300 Navigator", "S-NAV 310 Navigator"
										Msg = "The battery doesn't seem to fit"
										MsgTimer = 70 * 5
									Case "S-NAV Navigator Ultimate"
										Msg = "There seems to be no place for batteries in the navigator"
										MsgTimer = 70 * 5
									Case "Radio Transceiver"
										Select Inventory(MouseSlot)\itemtemplate\tempname 
											Case "fineradio", "veryfineradio"
												Msg = "There seems to be no place for batteries in the radio"
												MsgTimer = 70 * 5		
											Case "18vradio"
												If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))	
												RemoveItem (SelectedItem)
												SelectedItem = Null
												Inventory(MouseSlot)\state = 100.0
												Msg = "You replaced the battery of the radio"
												MsgTimer = 70 * 5
										End Select 
									Default
										Msg = "This item can't be used this way"
										MsgTimer = 70 * 5	
								End Select
							Default
								Msg = "This item can't be used this way"
								MsgTimer = 70 * 5
						End Select					
					End If
					
				End If
				SelectedItem = Null
			End If
		End If
		
		If Fullscreen Then DrawImage CursorIMG, MouseX(),MouseY()
		
		If InvOpen = False Then 
			ResumeSounds() 
			MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
		EndIf
	Else ;invopen = False
		
		If SelectedItem <> Null Then
			Select SelectedItem\itemtemplate\tempname
					
					;BoH Items
					;[Block]
				Case "nvgoggles", "supernv"
					;PlaySound_Strict PickSFX(SelectedItem\itemtemplate\sound)
					If WearingNightVision > 0 Then
						Msg = "You took off the goggles."
						CameraFogFar = StoredCameraFogFar
					Else
						Msg = "You put on the goggles and can see easier."
						WearingGasMask = 0
						Wearing178 = False
						StoredCameraFogFar = CameraFogFar
						CameraFogFar = 30
					EndIf
					If SelectedItem\itemtemplate\tempname="nvgoggles" Then
						If WearingNightVision=0 Then WearingNightVision = 1 Else WearingNightVision=0
					ElseIf SelectedItem\itemtemplate\tempname="supernv"
						If WearingNightVision=0 Then WearingNightVision = 2 Else WearingNightVision=0
					Else
						WearingNightVision = (Not WearingNightVision)
					EndIf
					SelectedItem = Null	
				Case "scp178"
					If Wearing178=1 Then
						Msg = "You took off the glasses"
						Wearing178 = 0
					Else
						GiveAchievement(Achv178)
						Msg = "You put on the glasses"
						Wearing178 = 1
						WearingGasMask = 0
						If WearingNightVision Then CameraFogFar = StoredCameraFogFar
						WearingNightVision = 0
					EndIf
					MsgTimer = 70 * 5
					SelectedItem = Null	
				Case "book"
					;Achievements(Achv1025)=True 
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\state = Rand(0,5)
						SelectedItem\itemtemplate\img=LoadImage("GFX\items\1025\1025_"+Int(SelectedItem\state)+".jpg")	
						SelectedItem\itemtemplate\img=ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
						
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					;SCP1025state[SelectedItem\state]=Max(1,SCP1025state[SelectedItem\state])					
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
					
				Case "ring"
					If Wearing714=2 Then
						Msg = "You took off the ring."
						Wearing714 = False
					Else
						;Achievements(Achv714)=True
						Msg = "You put on the ring."
						Wearing714 = 2
					EndIf
					MsgTimer = 70 * 5
					SelectedItem = Null	
				Case "pill"
					Local chanceCure% = Rand(100)
					If chanceCure<30 Then
						If Injuries > 0 And Infect > 0 And Bloodloss > 0 Then
							Msg = "You feel all your wounds and sicknesses heal"
						ElseIf Injuries > 0 And Bloodloss > 0
							Msg = "You feel all your wounds heal"
						Else
							Msg = "You feel better"	
						EndIf
						MsgTimer = 70*7
						
						Injuries = 0
						Bloodloss = 0
						Infect = 0
						Stamina = 100
						For i = 0 To 5
							SCP1025state[i]=0
						Next
						
						For e.Events = Each Events
							If e\EventName="room009" Then e\EventState=0.0 : e\EventState3=0.0
						Next
						
					ElseIf chanceCure<90 Then
						If Infect Then Msg = "The pain stopped increasing" : MsgTimer = 70*7
						For e.Events = Each Events
							If e\EventName="room009" Then
								If e\EventState>0.0 Then Msg = "The pain stopped increasing" : MsgTimer = 70*7
								e\EventState=0.0 : e\EventState3=0.0
							EndIf
						Next
					EndIf
					RemoveItem(SelectedItem)
					SelectedItem=Null
					;[End Block]
					
				Case "battery"
					;InvOpen = True
				Case "key1", "key2", "key3", "key4", "key5", "key6", "keyomni", "scp860", "hand", "hand2", "scp005"
					DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
				Case "scp513"
					PlaySound_Strict LoadTempSound("SFX\Bell1.ogg")
					
					temp = True
					For np.NPCs = Each NPCs
						If np\NPCtype = NPCtype5131 Then
							temp = False
							Exit
						EndIf
					Next
					If temp = True Then
						CreateNPC(NPCtype5131, 0,0,0)
					EndIf	
					SelectedItem = Null
				Case "scp500"
					GiveAchievement(Achv500)
					
					If Injuries > 0 And Infect > 0 And Bloodloss > 0 Then
						Msg = "You feel all your wounds and sicknesses heal"
					ElseIf Injuries > 0 And Bloodloss > 0
						Msg = "You feel all your wounds heal"
					Else
						Msg = "You feel better"	
					EndIf
					MsgTimer = 70*7
					
					DeathTimer=0
					Injuries = 0
					Bloodloss = 0
					Infect = 0
					Stamina = 100
					For i = 0 To 5
						SCP1025state[i]=0
					Next
					
					For e.Events = Each Events
						If e\EventName="room009" Then e\EventState=0.0 : e\EventState3=0.0
					Next
					
					
					RemoveItem(SelectedItem)
					SelectedItem = Null
					
				Case "veryfinefirstaid"
					Select Rand(5)
						Case 1
							Injuries = 3.5
							Msg = "You started bleeding heavily"
							MsgTimer = 70*7
						Case 2
							Injuries = 0
							Bloodloss = 0
							Msg = "Your wounds started healing up rapidly"
							MsgTimer = 70*7
						Case 3
							Injuries = Max(0, Injuries - Rnd(0.5,3.5))
							Bloodloss = Max(0, Bloodloss - Rnd(10,100))
							Msg = "You feel much better"
							MsgTimer = 70*7
						Case 4
							BlurTimer = 10000
							Bloodloss = 0
							Msg = "You feel nauseated"
							MsgTimer = 70*7
						Case 5
							BlinkTimer = -10
							For r.Rooms = Each Rooms
								If r\RoomTemplate\Name = "pocketdimension" Then
									PositionEntity(Collider, EntityX(r\obj),0.8,EntityZ(r\obj))		
									ResetEntity Collider									
									UpdateDoors()
									UpdateRooms()
									PlaySound_Strict(Use914SFX)
									DropSpeed = 0
									Curr106\State = -2500
									Exit
								EndIf
							Next
							Msg = "You got a sudden headache"
							MsgTimer = 70*8
					End Select
					
					RemoveItem(SelectedItem)
				Case "firstaid", "finefirstaid", "firstaid2"
					If Bloodloss = 0 And Injuries = 0 Then
						Msg = "You don't need to use the kit now"
						MsgTimer = 70*5
						SelectedItem = Null
					Else
						CurrSpeed = CurveValue(0, CurrSpeed, 5.0)
						Crouch = True
						
						DrawImage(SelectedItem\itemtemplate\invimg, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\invimg) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\invimg) / 2)
						
						width% = 300
						height% = 20
						x% = GraphicWidth / 2 - width / 2
						y% = GraphicHeight / 2 + 80
						Rect(x, y, width+4, height, False)
						For  i% = 1 To Int((width - 2) * (SelectedItem\state / 100.0) / 10)
							DrawImage(BlinkMeterIMG, x + 3 + 10 * (i - 1), y + 3)
						Next
						
						SelectedItem\state = Min(SelectedItem\state+(FPSfactor/5.0),100)			
						
						If SelectedItem\state = 100 Then
							If SelectedItem\itemtemplate\tempname = "finefirstaid" Then
								Bloodloss = 0
								Injuries = Max(0, Injuries - 2.0)
								If Injuries = 0 Then
									Msg = "You bandaged the wounds and took a painkiller. You feel fine."
								ElseIf Injuries > 1.0
									Msg = "You bandaged the wounds and took a painkiller, but you're still bleeding slightly."
								Else
									Msg = "You bandaged the wounds and took a painkiller, but you're still feeling sore."
								EndIf
								MsgTimer = 70*5
								RemoveItem(SelectedItem)
							Else
								Bloodloss = Max(0, Bloodloss - Rand(10,20))
								If Injuries => 2.5 Then
									Msg = "The wounds were way too severe to staunch the bleeding completely."
									Injuries = Max(2.5, Injuries-Rnd(0.3,0.7))
								ElseIf Injuries > 1.0
									Injuries = Max(0.5, Injuries-Rnd(0.5,1.0))
									If Injuries > 1.0 Then
										Msg = "You bandaged the wounds but were unable to staunch the bleeding completely."
									Else
										Msg = "You managed to stop the bleeding."
									EndIf
								Else
									If Injuries > 0.5 Then
										Injuries = 0.5
										Msg = "You took a painkiller. It eased the pain slightly."
									Else
										Injuries = 0.5
										Msg = "You took a painkiller, but it's still painful to walk."
									EndIf
								EndIf
								
								If SelectedItem\itemtemplate\tempname = "firstaid2" Then 
									Select Rand(6)
										Case 1
											SuperMan = True
											Msg = "You feel strange."
										Case 2
											InvertMouse = (Not InvertMouse)
											Msg = "You feel strange."
										Case 3
											BlurTimer = 5000
										Case 4
											EyeSuper = 70*Rand(20,30)
										Case 5
											Bloodloss = 0
											Injuries = 0
											Msg = "You bandaged the wounds. The bleeding stopped completely and you're feeling fine."
										Case 6
											Msg = "You bandaged the wounds and blood started pouring heavily through the bandages."
											Injuries = 3.5
									End Select
								EndIf
								
								MsgTimer = 70*5
								RemoveItem(SelectedItem)
							EndIf							
						EndIf
						
					EndIf
				Case "eyedrops"
					If (Not (Wearing714=1)) Then
						BlinkEffect = 0.6
						BlinkEffectTimer = 70*Rand(20,30)
						BlurTimer = 200
					EndIf
					RemoveItem(SelectedItem)
				Case "fineeyedrops"
					If (Not (Wearing714=1)) Then 
						BlinkEffect = 0.4
						BlinkEffectTimer = 70*Rand(30,40)
						Bloodloss = Max(Bloodloss-1.0, 0)
						BlurTimer = 200
					EndIf
					RemoveItem(SelectedItem)
				Case "supereyedrops"
					If (Not (Wearing714=1)) Then
						BlinkEffect = 0.0
						BlinkEffectTimer = 60
						EyeStuck = 10000
					EndIf
					BlurTimer = 1000
					RemoveItem(SelectedItem)					
				Case "paper"
					If SelectedItem\itemtemplate\img=0 Then
						Select SelectedItem\itemtemplate\name
							Case "Burnt Note" 
								SelectedItem\itemtemplate\img = LoadImage_Strict("GFX\items\bn.it")
								SetBuffer ImageBuffer(SelectedItem\itemtemplate\img)
								Color 0,0,0
								Text 277, 469, AccessCode, True, True
								Color 255,255,255
								SetBuffer BackBuffer()
							Case "Document SCP-372"
								SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
								SelectedItem\itemtemplate\img = ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
								
								SetBuffer ImageBuffer(SelectedItem\itemtemplate\img)
								Color 37,45,137
								SetFont font
								temp = ((Int(AccessCode)*3) Mod 10000)
								If temp < 1000 Then temp = temp+1000
								Text 333*MenuScale, 714*MenuScale, temp, True, True
								Color 255,255,255
								SetBuffer BackBuffer()
							Default 
								SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
								SelectedItem\itemtemplate\img = ResizeImage2(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
						End Select
						
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
				Case "scp1025"
					GiveAchievement(Achv1025) 
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\state = Rand(0,5)
						SelectedItem\itemtemplate\img=LoadImage_Strict("GFX\items\1025\1025_"+Int(SelectedItem\state)+".jpg")	
						ResizeImage(SelectedItem\itemtemplate\img, ImageWidth(SelectedItem\itemtemplate\img) * MenuScale, ImageHeight(SelectedItem\itemtemplate\img) * MenuScale)
						
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					SCP1025state[SelectedItem\state]=Max(1,SCP1025state[SelectedItem\state])					
					
					DrawImage(SelectedItem\itemtemplate\img, GraphicWidth / 2 - ImageWidth(SelectedItem\itemtemplate\img) / 2, GraphicHeight / 2 - ImageHeight(SelectedItem\itemtemplate\img) / 2)
					
				Case "cup"
					
					SelectedItem\name = Trim(Lower(SelectedItem\name))
					If Left(SelectedItem\name, Min(6,Len(SelectedItem\name))) = "cup of" Then
						SelectedItem\name = Right(SelectedItem\name, Len(SelectedItem\name)-7)
					ElseIf Left(SelectedItem\name, Min(8,Len(SelectedItem\name))) = "a cup of" 
						SelectedItem\name = Right(SelectedItem\name, Len(SelectedItem\name)-9)
					EndIf
					
					;the state of refined items is more than 1.0 (fine setting increases it by 1, very fine doubles it)
					x2 = (SelectedItem\state+1.0)
					
					Local iniStr$ = "DATA\SCP-294.ini"
					
					Local loc% = GetINISectionLocation(iniStr, SelectedItem\name)
					
					Stop
					
					strtemp = GetINIString2(iniStr, loc, "message")
					If strtemp <> "" Then Msg = strtemp : MsgTimer = 70*6
					
					If GetINIInt2(iniStr, loc, "lethal") Then 
						DeathMSG = GetINIString2(iniStr, loc, "deathmessage")
						Kill()
					EndIf
					BlurTimer = GetINIInt2(iniStr, loc, "blur")*70*temp
					Injuries = Injuries + GetINIInt2(iniStr, loc, "damage")*temp
					Bloodloss = Bloodloss + GetINIInt2(iniStr, loc, "blood loss")*temp
					strtemp =  GetINIString2(iniStr, loc, "sound")
					If strtemp<>"" Then
						PlaySound_Strict LoadTempSound(strtemp)
					EndIf
					If GetINIInt2(iniStr, loc, "stomachache") Then SCP1025state[3]=1
					If GetINIInt2(iniStr, loc, "godmode") Then GodMode=True
					
					DeathTimer=GetINIInt2(iniStr, loc, "deathtimer")*70
					
					BlinkEffect = (BlinkEffect + Float(GetINIString2(iniStr, loc, "blinkeffect", 1.0))*x2)/2.0
					BlinkEffectTimer = (BlinkEffectTimer + Float(GetINIString2(iniStr, loc, "blinkeffecttimer", 1.0))*x2)/2.0
					
					StaminaEffect = (StaminaEffect + Float(GetINIString2(iniStr, loc, "stamina effect", 1.0))*x2)/2.0
					StaminaEffectTimer = (StaminaEffectTimer + Float(GetINIString2(iniStr, loc, "staminaeffecttimer", 1.0))*x2)/2.0
					
					strtemp = GetINIString2(iniStr, loc, "refusemessage")
					If strtemp <> "" Then
						Msg = strtemp 
						MsgTimer = 70*6		
					Else
						it.Items = CreateItem("Empty Cup", "emptycup", 0,0,0)
						it\Picked = True
						For i = 0 To MaxItemAmount-1
							If Inventory(i)=SelectedItem Then Inventory(i) = it : Exit
						Next					
						EntityType (it\obj, HIT_ITEM)
						
						RemoveItem(SelectedItem)						
					EndIf
					
				Case "radio","18vradio","fineradio","veryfineradio"
					If SelectedItem\state <= 100 Then SelectedItem\state = Max(0, SelectedItem\state - FPSfactor * 0.004)
					
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					;radiostate(5) = has the "use the number keys" -message been shown yet (true/false)
					;radiostate(6) = a timer for the "code channel"
					;RadioState(7) = another timer for the "code channel"
					
					If RadioState(5) = 0 Then 
						Msg = "Use keys 1-5 to change the channel"
						MsgTimer = 70 * 5
						RadioState(5) = 1
					EndIf
					
					strtemp$ = ""
					
					x = GraphicWidth - ImageWidth(SelectedItem\itemtemplate\img) ;+ 120
					y = GraphicHeight - ImageHeight(SelectedItem\itemtemplate\img) ;- 30
					
					DrawImage(SelectedItem\itemtemplate\img, x, y)
					
					If SelectedItem\state > 0 Then
						If PlayerRoom\RoomTemplate\Name = "pocketdimension" Or CoffinDistance < 4.0 Then
							ResumeChannel(RadioCHN(0))
							If ChannelPlaying(RadioCHN(0)) = False Then RadioCHN(0) = PlaySound_Strict(RadioStatic)	
						Else
							Select Int(SelectedItem\state2)
								Case 0 ;randomkanava
									ResumeChannel(RadioCHN(0))
									If ChannelPlaying(RadioCHN(0)) = False Then RadioCHN(0) = PlaySound_Strict(RadioStatic)
								Case 1 ;h‰lytyskanava
									DebugLog RadioState(1) 
									
									ResumeChannel(RadioCHN(1))
									strtemp = "        WARNING - CONTAINMENT BREACH          "
									If ChannelPlaying(RadioCHN(1)) = False Then
										
										If RadioState(1) => 5 Then
											RadioCHN(1) = PlaySound_Strict(RadioSFX(1,1))	
											RadioState(1) = 0
										Else
											RadioState(1)=RadioState(1)+1	
											RadioCHN(1) = PlaySound_Strict(RadioSFX(1,0))	
										EndIf
										
									EndIf
									
								Case 2 ;scp-radio
									ResumeChannel(RadioCHN(2))
									strtemp = "        SCP Foundation On-Site Radio          "
									If ChannelPlaying(RadioCHN(2)) = False Then
										RadioState(2)=RadioState(2)+1
										If RadioState(2) = 17 Then RadioState(2) = 1
										If Floor(RadioState(2)/2)=Ceil(RadioState(2)/2) Then ;parillinen, soitetaan normiviesti
											RadioCHN(2) = PlaySound_Strict(RadioSFX(2,Int(RadioState(2)/2)))	
										Else ;pariton, soitetaan musiikkia
											RadioCHN(2) = PlaySound_Strict(RadioSFX(2,0))
										EndIf
									EndIf 
								Case 3
									ResumeChannel(RadioCHN(3))
									strtemp = "             EMERGENCY CHANNEL - RESERVED FOR COMMUNICATION IN THE EVENT OF A CONTAINMENT BREACH         "
									If ChannelPlaying(RadioCHN(3)) = False Then RadioCHN(3) = PlaySound_Strict(RadioStatic)
									
									If MTFtimer > 0 Then 
										RadioState(3)=RadioState(3)+Max(Rand(-10,1),0)
										Select RadioState(3)
											Case 40
												RadioCHN(3) = PlaySound_Strict(LoadTempSound("SFX\MTF\Random1.ogg"))
												RadioState(3)=RadioState(3)+1													
											Case 400
												RadioCHN(3) = PlaySound_Strict(LoadTempSound("SFX\MTF\Random2.ogg"))
												RadioState(3)=RadioState(3)+1	
											Case 800
												RadioCHN(3) = PlaySound_Strict(LoadTempSound("SFX\MTF\Random3.ogg"))
												RadioState(3)=RadioState(3)+1															
											Case 1200
												RadioCHN(3) = PlaySound_Strict(LoadTempSound("SFX\MTF\Random4.ogg"))	
												RadioState(3)=RadioState(3)+1		
										End Select
									EndIf
								Case 4
									ResumeChannel(RadioCHN(6)) ;taustalle kohinaa
									If ChannelPlaying(RadioCHN(6)) = False Then RadioCHN(6) = PlaySound_Strict(RadioStatic)									
									
									ResumeChannel(RadioCHN(4))
									If ChannelPlaying(RadioCHN(4)) = False Then 
										If RemoteDoorOn = False And RadioState(8) = False Then
											RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter3.ogg"))	
											RadioState(8) = True
										Else
											RadioState(4)=RadioState(4)+Max(Rand(-10,1),0)
											
											Select RadioState(4)
												Case 10
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\OhGod.ogg"))
													RadioState(4)=RadioState(4)+1													
												Case 100
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter2.ogg"))
													RadioState(4)=RadioState(4)+1	
												Case 158
													If MTFtimer = 0 Then 
														RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\franklin1.ogg"))
														RadioState(4)=RadioState(4)+1
													EndIf
												Case 200
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter4.ogg"))
													RadioState(4)=RadioState(4)+1
												Case 260
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\035\radio1.ogg"))
													RadioState(4)=RadioState(4)+1
												Case 300
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\Chatter1.ogg"))	
													RadioState(4)=RadioState(4)+1	
												Case 350
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\035\franklin2.ogg"))
													RadioState(4)=RadioState(4)+1
												Case 400
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\035\radio2.ogg"))
													RadioState(4)=RadioState(4)+1
												Case 450
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\franklin3.ogg"))	
													RadioState(4)=RadioState(4)+1		
												Case 600
													RadioCHN(4) = PlaySound_Strict(LoadTempSound("SFX\radio\franklin4.ogg"))	
													RadioState(4)=RadioState(4)+1	
											End Select
										EndIf
									EndIf
									
									
								Case 5
									ResumeChannel(RadioCHN(5))
									If ChannelPlaying(RadioCHN(5)) = False Then RadioCHN(5) = PlaySound_Strict(RadioStatic)
							End Select 
							
							x=x+66
							y=y+419
							
							Color (30,30,30)
							
							If SelectedItem\state <= 100 Then
								;Text (x - 60, y - 20, "BATTERY")
								For i = 0 To 4
									Rect(x, y+8*i, 43 - i * 6, 4, Ceil(SelectedItem\state / 20.0) > 4 - i )
								Next
							EndIf	
							
							SetFont Font3
							Text(x+60, y, "CHN")						
							
							If SelectedItem\itemtemplate\tempname = "veryfineradio" Then ;"KOODIKANAVA"
								ResumeChannel(RadioCHN(0))
								If ChannelPlaying(RadioCHN(0)) = False Then RadioCHN(0) = PlaySound_Strict(RadioStatic)
								
								;radiostate(7)=kuinka mones piippaus menossa
								;radiostate(8)=kuinka mones access coden numero menossa
								RadioState(6)=RadioState(6) + FPSfactor
								temp = Mid(Str(AccessCode),RadioState(8)+1,1)
								If RadioState(6)-FPSfactor =< RadioState(7)*50 And RadioState(6)>RadioState(7)*50 Then
									PlaySound_Strict(RadioBuzz)
									RadioState(7)=RadioState(7)+1
									If RadioState(7)=>temp Then
										RadioState(7)=0
										RadioState(6)=-100
										RadioState(8)=RadioState(8)+1
										If RadioState(8)=4 Then RadioState(8)=0 : RadioState(6)=-200
									EndIf
								EndIf
								
								strtemp = ""
								For i = 0 To Rand(5, 30)
									strtemp = strtemp + Chr(Rand(1,100))
								Next
								
								SetFont Font4
								Text(x+97, y+16, Rand(0,9),True,True)
								
							Else
								For i = 2 To 6
									If KeyHit(i) Then
										If SelectedItem\state2 <> i-2 Then ;pausetetaan nykyinen radiokanava
											PlaySound_Strict RadioSquelch
											If RadioCHN(Int(SelectedItem\state2)) <> 0 Then PauseChannel(RadioCHN(Int(SelectedItem\state2)))
										EndIf
										SelectedItem\state2 = i-2
										;jos nykyist‰ kanavaa ollaan soitettu, laitetaan jatketaan toistoa samasta kohdasta
										If RadioCHN(SelectedItem\state2)<>0 Then ResumeChannel(RadioCHN(SelectedItem\state2))
									EndIf
								Next
								
								SetFont Font4
								Text(x+97, y+16, Int(SelectedItem\state2+1),True,True)
							EndIf
							
							SetFont Font3
							If strtemp <> "" Then
								strtemp = Right(Left(strtemp, (Int(MilliSecs()/300) Mod Len(strtemp))),10)
								Text(x+32, y+33, strtemp)
							EndIf
							
							SetFont Font1
							
						EndIf
						
					EndIf
					
				Case "cigarette"
					Msg = "I don't have anything to light it with. Umm, what about that... Nevermind."
					MsgTimer = 70 * 5
					RemoveItem(SelectedItem)
				Case "420"
					If Wearing714=1 Then
						Msg = "DUDE WTF THIS SHIT DOESN'T EVEN WORK"	
					Else
						Msg = "MAN DATS SUM GOOD ASS SHIT"
						Injuries = Max(Injuries-0.5, 0)
						BlurTimer = 500
						GiveAchievement(Achv420)
						PlaySound_Strict LoadTempSound("SFX\Mandeville.ogg")
					EndIf
					MsgTimer = 70 * 5
					RemoveItem(SelectedItem)
				Case "420s"
					If Wearing714=1 Then
						Msg = "DUDE WTF THIS SHIT DOESN'T EVEN WORK"	
					Else
						DeathMSG = "Subject D-9341 found in a comatose state in [DATA REDACTED]. The subject was holding what appears to be a cigarette and smiling widely. "
						DeathMSG = DeathMSG+"Chemical analysis of the cigarette has been inconclusive, although it seems to contain a high concentration of an unidentified chemical "
						DeathMSG = DeathMSG+"whose molecular structure is remarkably similar to that of tetrahydrocannabinol."
						Msg = "UH WHERE... WHAT WAS I DOING AGAIN... MAN I NEED TO TAKE A NAP..."
						KillTimer = -1						
					EndIf
					MsgTimer = 70 * 6
					RemoveItem(SelectedItem)
				Case "scp714"
					If Wearing714=1 Then
						Msg = "You took off the ring."
						Wearing714 = False
					Else
						GiveAchievement(Achv714)
						Msg = "You put on the ring."
						Wearing714 = True
					EndIf
					MsgTimer = 70 * 5
					SelectedItem = Null	
				Case "hazmatsuit", "hazmatsuit2", "hazmatsuit3"
					If WearingHazmat Then
						Msg = "You take off the hazmat suit."
					Else
						Msg = "You put on the hazmat suit."
					EndIf
					MsgTimer = 70 * 5
					If SelectedItem\itemtemplate\tempname="hazmatsuit3" Then
						If WearingHazmat=0 Then WearingHazmat = 3 Else WearingHazmat=0
					ElseIf SelectedItem\itemtemplate\tempname="hazmatsuit2"
						If WearingHazmat=0 Then WearingHazmat = 2 Else WearingHazmat=0
					Else
						WearingHazmat = (Not WearingHazmat)
					EndIf
					SelectedItem = Null	
				Case "vest"
					If WearingVest Then
						Msg = "You took off the vest."
						WearingVest = False
					Else
						Msg = "You put on the vest and feel slightly encumbered."
						WearingVest = True
					EndIf
					MsgTimer = 70 * 7
					SelectedItem = Null
				Case "finevest"
					If WearingVest Then
						Msg = "You took off the vest."
						WearingVest = False						
					Else
						Msg = "You put on the vest and feel heavily encumbered."
						WearingVest = 2
					EndIf
					SelectedItem = Null	
				Case "gasmask", "supergasmask", "gasmask3"
					If WearingGasMask Then
						Msg = "You took off the gas mask."
					Else
						Msg = "You put on the gas mask."
						Wearing178 = 0
						If WearingNightVision Then CameraFogFar = StoredCameraFogFar
						WearingNightVision = 0
					EndIf
					MsgTimer = 70 * 5
					If SelectedItem\itemtemplate\tempname="gasmask3" Then
						If WearingGasMask=0 Then WearingGasMask = 3 Else WearingGasMask=0
					ElseIf SelectedItem\itemtemplate\tempname="supergasmask"
						If WearingGasMask=0 Then WearingGasMask = 2 Else WearingGasMask=0
					Else
						WearingGasMask = (Not WearingGasMask)
					EndIf
					SelectedItem = Null				
				Case "navigator", "nav"
					
					If SelectedItem\itemtemplate\img=0 Then
						SelectedItem\itemtemplate\img=LoadImage_Strict(SelectedItem\itemtemplate\imgpath)	
						MaskImage(SelectedItem\itemtemplate\img, 255, 0, 255)
					EndIf
					
					If SelectedItem\state <= 100 Then SelectedItem\state = Max(0, SelectedItem\state - FPSfactor * 0.005)
					
					x = GraphicWidth - ImageWidth(SelectedItem\itemtemplate\img)*0.5+20
					y = GraphicHeight - ImageHeight(SelectedItem\itemtemplate\img)*0.4-85
					width = 287
					height = 256
					
					DrawImage(SelectedItem\itemtemplate\img, x - ImageWidth(SelectedItem\itemtemplate\img) / 2, y - ImageHeight(SelectedItem\itemtemplate\img) / 2 + 85)
					
					SetFont Font3
					
					If PlayerRoom\RoomTemplate\Name = "pocketdimension" Then
						If (MilliSecs() Mod 1000) > 300 Then	
							Text(x, y + height / 2 - 80, "ERROR 06", True)
							Text(x, y + height / 2 - 60, "LOCATION UNKNOWN", True)						
						EndIf
					Else
						
						If SelectedItem\state > 0 And (Rnd(CoffinDistance + 15.0) > 1.0 Or PlayerRoom\RoomTemplate\Name <> "coffin") Then
							
							If SelectedItem\itemtemplate\name = "S-NAV Navigator" Then 
								Color(100, 0, 0)
							Else
								Color (30,30,30)
							EndIf
							If (MilliSecs() Mod 1000) > 300 Then
								If SelectedItem\itemtemplate\name <> "S-NAV 310 Navigator" And SelectedItem\itemtemplate\name <> "S-NAV Navigator Ultimate" Then
									Text(x, y + height / 2 - 40, "COULD NOT CONNECT", True)
									Text(x, y + height / 2 - 20, "TO MAP DATABASE", True)
								EndIf
								
								yawvalue = EntityYaw(Collider)-90
								x1 = x+Cos(yawvalue)*6 : y1 = y-Sin(yawvalue)*6
								x2 = x+Cos(yawvalue-140)*5 : y2 = y-Sin(yawvalue-140)*5				
								x3 = x+Cos(yawvalue+140)*5 : y3 = y-Sin(yawvalue+140)*5
								
								Line x1,y1,x2,y2
								Line x1,y1,x3,y3
								Line x2,y2,x3,y3
							EndIf
							
							Local PlayerX% = Floor(EntityX(PlayerRoom\obj) / 8.0 + 0.5), PlayerZ% = Floor(EntityZ(PlayerRoom\obj) / 8.0 + 0.5)
							If SelectedItem\itemtemplate\name = "S-NAV Navigator Ultimate" And (MilliSecs() Mod 600) < 400 Then
								Local dist# = EntityDistance(Camera, Curr173\obj)
								dist = Ceil(dist / 8.0) * 8.0
								If dist < 8.0 * 4 Then
									Color 100, 0, 0
									Oval(x - dist * 3, y - 7 - dist * 3, dist * 3 * 2, dist * 3 * 2, False)
									Text(x - width / 2 + 20, y - height / 2 + 20, "SCP-173")
								EndIf
								dist# = EntityDistance(Camera, Curr106\obj)
								If dist < 8.0 * 4 Then
									Color 100, 0, 0
									Oval(x - dist * 1.5, y - 7 - dist * 1.5, dist * 3, dist * 3, False)
									Text(x - width / 2 + 20, y - height / 2 + 40, "SCP-106")
								EndIf
								If Curr096<>Null Then 
									dist# = EntityDistance(Camera, Curr096\obj)
									If dist < 8.0 * 4 Then
										Color 100, 0, 0
										Oval(x - dist * 1.5, y - 7 - dist * 1.5, dist * 3, dist * 3, False)
										Text(x - width / 2 + 20, y - height / 2 + 40, "SCP-096")
									EndIf
								EndIf
								
								If PlayerRoom\RoomTemplate\Name = "coffin" Then
									If CoffinDistance < 8.0 Then
										dist = Rnd(4.0, 8.0)
										Color 100, 0, 0
										Oval(x - dist * 1.5, y - 7 - dist * 1.5, dist * 3, dist * 3, False)
										Text(x - width / 2 + 20, y - height / 2 + 40, "SCP-895")
									EndIf
								EndIf
							End If
							
							Color (30,30,30)
							If SelectedItem\itemtemplate\name = "S-NAV Navigator" Then Color(100, 0, 0)
							If SelectedItem\state <= 100 Then
								Text (x - width/2 + 10, y - height/2 + 10, "BATTERY")
								xtemp = x - width/2 + 10
								ytemp = y - height/2 + 30		
								Line xtemp, ytemp, xtemp+20, ytemp
								Line xtemp, ytemp+100, xtemp+20, ytemp+100
								Line xtemp, ytemp, xtemp, ytemp+100
								Line xtemp+20, ytemp, xtemp+20, ytemp+100
								
								SetFont Font4
								For i = 1 To Ceil(SelectedItem\state / 10.0)
									Text (xtemp+11, ytemp+i*10-26, "-", True)
									;Rect(x - width/2, y+i*15, 40 - i * 6, 5, Ceil(SelectedItem\state / 20.0) > 4 - i)
								Next
								SetFont Font3
							EndIf
							
							x = x - 19 + ((EntityX(Collider) - 4.0) Mod 8.0)*3
							y = y + 14 - ((EntityZ(Collider)-4.0) Mod 8.0)*3
							For x2 = Max(1, PlayerX - 4) To Min(MapWidth - 1, PlayerX + 4)
								For z2 = Max(1, PlayerZ - 4) To Min(MapHeight - 1, PlayerZ + 4)
									
									If CoffinDistance > 16.0 Or Rnd(16.0)<CoffinDistance Then 
										If MapTemp(x2, z2) And (MapFound(x2, z2) > 0 Or SelectedItem\itemtemplate\name = "S-NAV 310 Navigator" Or SelectedItem\itemtemplate\name = "S-NAV Navigator Ultimate") Then
											Local drawx% = x + (PlayerX - x2) * 24 , drawy% = y - (PlayerZ - z2) * 24 
											
											Color (30,30,30)
											If SelectedItem\itemtemplate\name = "S-NAV Navigator" Then Color(100, 0, 0)
											
											If MapTemp(x2 + 1, z2) = False Then Line(drawx - 12, drawy - 12, drawx - 12, drawy + 12)
											If MapTemp(x2 - 1, z2) = False Then Line(drawx + 12, drawy - 12, drawx + 12, drawy + 12)
											
											If MapTemp(x2, z2 - 1) = False Then Line(drawx - 12, drawy - 12, drawx + 12, drawy - 12)
											If MapTemp(x2, z2 + 1)= False Then Line(drawx - 12, drawy + 12, drawx + 12, drawy + 12)
											
										End If
									EndIf
									
								Next
							Next
							
						EndIf
						
					EndIf
				;new Items in SCP:CB 1.3
				Case "scp1499"
					If NTF_Wearing1499% Then
						Msg = "You took off SCP-1499 and you reappeared in the facility."
						For r.Rooms = Each Rooms
							If r\RoomTemplate\Name = NTF_1499PrevRoom$ Then
								NTF_1499X# = EntityX(Collider)
								NTF_1499Y# = EntityY(Collider)
								NTF_1499Z# = EntityZ(Collider)
								PositionEntity (Collider, NTF_1499PrevX#, NTF_1499PrevY#+0.05, NTF_1499PrevZ#)
								ResetEntity(Collider)
								UpdateDoors()
								UpdateRooms()
								For it.Items = Each Items
									it\disttimer = 0
								Next
								PlayerRoom = r
								PlaySound_Strict NTF_1499LeaveSFX%
								Exit
							EndIf
						Next
					Else
						Msg = "You took on SCP-1499 and you appeared in a strange dimension."
						Wearing178 = 0
						WearingGasMask = 0
						If WearingNightVision Then CameraFogFar = StoredCameraFogFar
						WearingNightVision = 0
						For r.Rooms = Each Rooms
							If r\RoomTemplate\Name = "dimension1499" Then
								NTF_1499PrevRoom = PlayerRoom\RoomTemplate\Name
								NTF_1499PrevX# = EntityX(Collider)
								NTF_1499PrevY# = EntityY(Collider)
								NTF_1499PrevZ# = EntityZ(Collider)
								
								If NTF_1499X# = 0.0 And NTF_1499Y# = 0.0 And NTF_1499Z# = 0.0
									PositionEntity (Collider, r\x+15616.0*RoomScale, r\y+192.0*RoomScale, r\z-1536.0*RoomScale)
								Else
									PositionEntity (Collider, NTF_1499X#, NTF_1499Y#+0.05, NTF_1499Z#)
								EndIf
								ResetEntity(Collider)
								UpdateDoors()
								UpdateRooms()
								For it.Items = Each Items
									it\disttimer = 0
								Next
								PlayerRoom = r
								PlaySound_Strict NTF_1499EnterSFX%
								Exit
							EndIf
						Next
					EndIf
					MsgTimer = 70 * 5
					NTF_Wearing1499% = (Not NTF_Wearing1499%)
					SelectedItem = Null	
				Default
					;check if the item is an inventory-type object
					If SelectedItem\invSlots>0 Then
						DoubleClick = 0
						MouseHit1 = 0
						MouseDown1 = 0
						LastMouseHit1 = 0
						OtherOpen = SelectedItem
						SelectedItem = Null
					EndIf
					
			End Select
			
			If MouseHit2 Then
				EntityAlpha Dark, 0.0
				
				If SelectedItem\itemtemplate\tempname = "paper" Or SelectedItem\itemtemplate\tempname = "scp1025"  Then
					If SelectedItem\itemtemplate\img<>0 Then FreeImage(SelectedItem\itemtemplate\img)
					SelectedItem\itemtemplate\img=0
				EndIf
				
				If SelectedItem\itemtemplate\sound <> 66 Then PlaySound_Strict(PickSFX(SelectedItem\itemtemplate\sound))
				SelectedItem = Null
			EndIf
		End If		
	EndIf
	
	If SelectedItem = Null Then
		For i = 0 To 6
			If RadioCHN(i) <> 0 Then 
				If ChannelPlaying(RadioCHN(i)) Then PauseChannel(RadioCHN(i))
			EndIf
		Next
	EndIf 
	
	If PrevInvOpen And (Not InvOpen) Then MoveMouse viewport_center_x, viewport_center_y
End Function

Function DrawMenu()
	Local x%, y%, width%, height%
	
	If MenuOpen Then
		
		If StopHidingTimer = 0 Then
			If EntityDistance(Curr173\Collider, Collider)<4.0 Or EntityDistance(Curr106\Collider, Collider)<4.0 Then 
				StopHidingTimer = 1
			EndIf	
		ElseIf StopHidingTimer < 40
			If KillTimer >= 0 Then 
				StopHidingTimer = StopHidingTimer+FPSfactor
				
				If StopHidingTimer => 40 Then
					PlaySound_Strict(HorrorSFX(15))
					Msg = "STOP HIDING"
					MsgTimer = 6*70
					MenuOpen = False
					Return
				EndIf
			EndIf
		EndIf
		
		InvOpen = False
		
		width = ImageWidth(PauseMenuIMG)
		height = ImageHeight(PauseMenuIMG)
		x = GraphicWidth / 2 - width / 2
		y = GraphicHeight / 2 - height / 2
		
		DrawImage PauseMenuIMG, x, y
		
		Color(255, 255, 255)
		
		x = x+132*MenuScale
		y = y+122*MenuScale	
		
		If AchievementsMenu Then
			SetFont Font2
			Text(x, y-(122-45)*MenuScale, "ACHIEVEMENTS",False,True)
			SetFont Font1
		ElseIf KillTimer >= 0 Then
			SetFont Font2
			Text(x, y-(122-45)*MenuScale, "PAUSED",False,True)
			SetFont Font1
		Else
			SetFont Font2
			Text(x, y-(122-45)*MenuScale, "YOU DIED",False,True)
			SetFont Font1
		End If		
		
		Local AchvXIMG% = (x + (22*MenuScale))
		Local scale# = GraphicHeight/768.0
		Local SeparationConst% = 76*scale
		Local imgsize% = 64
		
		If AchievementsMenu <= 0 Then
			SetFont Font1
			Text x, y, "Designation: D-9341"
			Text x, y+20*MenuScale, "Difficulty: "+SelectedDifficulty\name
			Text x, y+40*MenuScale,	"Save: "+CurrSave
			Text x, y+60*MenuScale, "Map seed: "+RandomSeed
		Else
			If DrawButton(x+101*MenuScale, y + 344*MenuScale, 230*MenuScale, 60*MenuScale, "Back") Then
				AchievementsMenu = 0
				MouseHit1 = False
			EndIf
			
			If AchievementsMenu>0 Then
				DebugLog AchievementsMenu
				If AchievementsMenu <= Floor(Float(MAXACHIEVEMENTS-1)/12.0) Then 
					If DrawButton(x+341*MenuScale, y + 344*MenuScale, 50*MenuScale, 60*MenuScale, ">") Then
						AchievementsMenu = AchievementsMenu+1
					EndIf
				EndIf
				If AchievementsMenu > 1 Then
					If DrawButton(x+41*MenuScale, y + 344*MenuScale, 50*MenuScale, 60*MenuScale, "<") Then
						AchievementsMenu = AchievementsMenu-1
					EndIf
				EndIf
				
				For i=0 To 11
					If i+((AchievementsMenu-1)*12)<MAXACHIEVEMENTS Then
						DrawAchvIMG(AchvXIMG,y+((i/4)*120*MenuScale),i+((AchievementsMenu-1)*12))
					Else
						Exit
					EndIf
				Next
				
				For i=0 To 11
					If i+((AchievementsMenu-1)*12)<MAXACHIEVEMENTS Then
						If MouseOn(AchvXIMG+((i Mod 4)*SeparationConst),y+((i/4)*120*MenuScale),64*scale,64*scale) Then
							AchievementTooltip(i+((AchievementsMenu-1)*12))
							Exit
						EndIf
					Else
						Exit
					EndIf
				Next
				
			EndIf
		EndIf
		
		y = y+10
		
		If AchievementsMenu<=0 Then
			If KillTimer >= 0 Then	
				y = y+ 104*MenuScale
				If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Resume") Then
					MenuOpen = False
					ResumeSounds()
					MouseXSpeed() : MouseYSpeed() : MouseZSpeed() : mouse_x_speed_1#=0.0 : mouse_y_speed_1#=0.0
				EndIf
				y = y + 80*MenuScale
				If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Achievements") Then AchievementsMenu = 1
				y = y + 80*MenuScale
				
				If SelectedDifficulty\saveType = SAVEONQUIT Or SelectedDifficulty\saveType = SAVEANYWHERE Then
					If PlayerRoom\RoomTemplate\Name <> "173" And PlayerRoom\RoomTemplate\Name <> "exit1" Then
						If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Save & quit") Then
							DropSpeed = 0
							SaveGame(SavePath + CurrSave + "\")
							NullGame()
							MenuOpen = False
							MainMenuOpen = True
							MainMenuTab = 0
							CurrSave = ""
							FlushKeys()	
						EndIf
					Else
						DrawButton(x, y, 390*MenuScale, 60*MenuScale, "")
						Color 50,50,50
						Text(x + 185*MenuScale, y + 30*MenuScale, "Save & quit", True, True)
					EndIf
					y= y + 80*MenuScale
				EndIf
			Else
				y = y+104*MenuScale
				
				If GameSaved Then
					If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Load game") Then
						DrawLoading(0)
						
						MenuOpen = False
						LoadGameQuick(SavePath + CurrSave + "\")
						
						MoveMouse viewport_center_x,viewport_center_y
						SetFont Font1
						HidePointer ()
						
						FlushKeys()
						FlushMouse()
						Playable=True
						
						UpdateRooms()
						
						For r.Rooms = Each Rooms
							x = Abs(EntityX(Collider) - EntityX(r\obj))
							z = Abs(EntityZ(Collider) - EntityZ(r\obj))
							
							If x < 12.0 And z < 12.0 Then
								MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)) = Max(MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)), 1)
								If x < 4.0 And z < 4.0 Then
									If Abs(EntityY(Collider) - EntityY(r\obj)) < 1.5 Then PlayerRoom = r
									MapFound(Floor(EntityX(r\obj) / 8.0), Floor(EntityZ(r\obj) / 8.0)) = 1
								EndIf
							End If
						Next
						
						DrawLoading(100)
						
						DropSpeed=0
						
						UpdateWorld 0.0
						
						PrevTime = MilliSecs()
						FPSfactor = 0	
					EndIf
				Else
					DrawButton(x, y, 390*MenuScale, 60*MenuScale, "")
					Color 50,50,50
					Text(x + 185*MenuScale, y + 30*MenuScale, "Load game", True, True)
				EndIf
				y= y + 80*MenuScale
			EndIf
			
			If DrawButton(x, y, 390*MenuScale, 60*MenuScale, "Quit") Then
				NullGame()
				MenuOpen = False
				MainMenuOpen = True
				MainMenuTab = 0
				CurrSave = ""
				FlushKeys()
			EndIf
			
			SetFont Font1
			If KillTimer < 0 Then RowText(DeathMSG$, x, y + 80*MenuScale, 390*MenuScale, 600*MenuScale)
		EndIf
		
		If Fullscreen Then DrawImage CursorIMG, MouseX(),MouseY()
		
	End If
	
	SetFont Font1
End Function

Function MouseOn%(x%, y%, width%, height%)
	If MouseX() > x And MouseX() < x + width Then
		If MouseY() > y And MouseY() < y + height Then
			Return True
		End If
	End If
	Return False
End Function

;----------------------------------------------------------------------------------------------

Function LoadEntities()
	DrawLoading(0)
	
	Local i%
	
	For i=0 To 9
		TempSounds[i]=0
	Next
	
	SoundEmitter = CreatePivot()
	
	Camera = CreateCamera()
	CameraRange(Camera, 0.05, 16)
	CameraFogMode (Camera, 1)
	CameraFogRange (Camera, CameraFogNear, CameraFogFar)
	CameraFogColor (Camera, GetINIInt("options.ini", "options", "fog r"), GetINIInt("options.ini", "options", "fog g"), GetINIInt("options.ini", "options", "fog b"))
	AmbientLight Brightness, Brightness, Brightness
	
	ScreenTexs[0] = CreateTexture(512, 512, 1+256+FE_RENDER+FE_ZRENDER)
	ScreenTexs[1] = CreateTexture(512, 512, 1+256+FE_RENDER+FE_ZRENDER)
	
	InitFastResize()
	
	CreateBlurImage()
	;Listener = CreateListener(Camera)
	
	FogTexture = LoadTexture_Strict("GFX\fog.jpg", 1)
	
	Fog = CreateSprite(ark_blur_cam)
	ScaleSprite(Fog, Max(GraphicWidth / 1240.0, 1.0), Max(GraphicHeight / 960.0 * 0.8, 0.8))
	EntityTexture(Fog, FogTexture)
	EntityBlend (Fog, 2)
	EntityOrder Fog, -1000
	MoveEntity(Fog, 0, 0, 1.0)
	
	GasMaskTexture = LoadTexture_Strict("GFX\GasmaskOverlay.jpg", 1)
	GasMaskOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(GasMaskOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(GasMaskOverlay, GasMaskTexture)
	EntityBlend (GasMaskOverlay, 2)
	EntityFX(GasMaskOverlay, 1)
	EntityOrder GasMaskOverlay, -1003
	MoveEntity(GasMaskOverlay, 0, 0, 1.0)
	HideEntity(GasMaskOverlay)
	
	InfectTexture = LoadTexture_Strict("GFX\InfectOverlay.jpg", 1)
	InfectOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(InfectOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(InfectOverlay, InfectTexture)
	EntityBlend (InfectOverlay, 3)
	EntityFX(InfectOverlay, 1)
	EntityOrder InfectOverlay, -1003
	MoveEntity(InfectOverlay, 0, 0, 1.0)
	;EntityAlpha (InfectOverlay, 255.0)
	HideEntity(InfectOverlay)
	
	NVTexture = LoadTexture_Strict("GFX\NightVisionOverlay.jpg", 1)
	NVOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(NVOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(NVOverlay, NVTexture)
	EntityBlend (NVOverlay, 2)
	EntityFX(NVOverlay, 1)
	EntityOrder NVOverlay, -1003
	MoveEntity(NVOverlay, 0, 0, 1.0)
	HideEntity(NVOverlay)
	
	GlassesTexture = LoadTexture_Strict("GFX\GlassesOverlay.jpg",1)
	GlassesOverlay = CreateSprite(ark_blur_cam)
	ScaleSprite(GlassesOverlay, Max(GraphicWidth / 1024.0, 1.0), Max(GraphicHeight / 1024.0 * 0.8, 0.8))
	EntityTexture(GlassesOverlay, GlassesTexture)
	EntityBlend (GlassesOverlay, 2)
	EntityFX(GlassesOverlay, 1)
	EntityOrder GlassesOverlay, -1003
	MoveEntity(GlassesOverlay, 0, 0, 1.0)
	HideEntity(GlassesOverlay)
	
	FogNVTexture = LoadTexture_Strict("GFX\fogNV.jpg", 1)
	
	DrawLoading(5)
	
	DarkTexture = CreateTexture(1024, 1024, 1 + 2)
	SetBuffer TextureBuffer(DarkTexture)
	Cls
	SetBuffer BackBuffer()
	
	Dark = CreateSprite(Camera)
	ScaleSprite(Dark, Max(GraphicWidth / 1240.0, 1.0), Max(GraphicHeight / 960.0 * 0.8, 0.8))
	EntityTexture(Dark, DarkTexture)
	EntityBlend (Dark, 1)
	EntityOrder Dark, -1002
	MoveEntity(Dark, 0, 0, 1.0)
	EntityAlpha Dark, 0.0
	
	LightTexture = CreateTexture(1024, 1024, 1 + 2)
	SetBuffer TextureBuffer(LightTexture)
	ClsColor 255, 255, 255
	Cls
	ClsColor 0, 0, 0
	SetBuffer BackBuffer()
	
	TeslaTexture = LoadTexture_Strict("GFX\map\tesla.jpg", 1+2)
	
	Light = CreateSprite(Camera)
	ScaleSprite(Light, Max(GraphicWidth / 1240.0, 1.0), Max(GraphicHeight / 960.0 * 0.8, 0.8))
	EntityTexture(Light, LightTexture)
	EntityBlend (Light, 1)
	EntityOrder Light, -1002
	MoveEntity(Light, 0, 0, 1.0)
	HideEntity Light
	
	Collider = CreatePivot()
	EntityRadius Collider, 0.15, 0.30
	EntityPickMode(Collider, 1)
	EntityType Collider, HIT_PLAYER
	
	Head = CreatePivot()
	EntityRadius Head, 0.15
	EntityType Head, HIT_PLAYER
	
	
	LiquidObj = LoadMesh_Strict("GFX\items\cupliquid.x") ;optimized the cups dispensed by 294
	HideEntity LiquidObj
	
	MTFObj = LoadAnimMesh_Strict("GFX\npcs\MTF2.b3d") ;optimized MTFs
	GuardObj = LoadAnimMesh_Strict("GFX\npcs\guard.b3d") ;optimized Guards
	;GuardTex = LoadTexture_Strict("GFX\npcs\body.jpg") ;optimized the guards even more
	
	If BumpEnabled Then
		bump1 = LoadTexture_Strict("GFX\npcs\mtf_newnormal01.png")
		TextureBlend bump1, FE_BUMP
			
		For i = 2 To CountSurfaces(MTFObj)
			sf = GetSurface(MTFObj,i)
			b = GetSurfaceBrush( sf )
			t1 = GetBrushTexture(b,0)
			
			Select Lower(StripPath(TextureName(t1)))
				Case "MTF_newdiffuse02.png"
					
					BrushTexture b, bump1, 0, 0
					BrushTexture b, t1, 0, 1
					PaintSurface sf,b
					
					If StripPath(TextureName(t1)) <> "" Then FreeTexture t1
					FreeBrush b	
			End Select
			FreeBrush b
			FreeTexture t1
		Next
		FreeTexture bump1	
	EndIf
	
	
	
	ClassDObj = LoadAnimMesh_Strict("GFX\npcs\classd.b3d") ;optimized Class-D's and scientists/researchers
	ApacheObj = LoadAnimMesh_Strict("GFX\apache.b3d") ;optimized Apaches (helicopters)
	ApacheRotorObj = LoadAnimMesh_Strict("GFX\apacherotor.b3d") ;optimized the Apaches even more
	
	HideEntity MTFObj
	HideEntity GuardObj
	HideEntity ClassDObj
	HideEntity ApacheObj
	HideEntity ApacheRotorObj
	
	LightSpriteTex(0) = LoadTexture_Strict("GFX\light1.jpg", 1)
	LightSpriteTex(1) = LoadTexture_Strict("GFX\light2.jpg", 1)
	LightSpriteTex(2) = LoadTexture_Strict("GFX\lightsprite.jpg",1)
	
	DrawLoading(10)
	
	DoorOBJ = LoadMesh_Strict("GFX\map\door01.x")
	HideEntity DoorOBJ
	DoorFrameOBJ = LoadMesh_Strict("GFX\map\doorframe.x")
	HideEntity DoorFrameOBJ
	
	HeavyDoorObj(0) = LoadMesh_Strict("GFX\map\heavydoor1.x")
	HideEntity HeavyDoorObj(0)
	HeavyDoorObj(1) = LoadMesh_Strict("GFX\map\heavydoor2.x")
	HideEntity HeavyDoorObj(1)
	
	DoorColl = LoadMesh_Strict("GFX\map\doorcoll.x")
	HideEntity DoorColl
	
	ButtonOBJ = LoadMesh_Strict("GFX\map\Button.x")
	HideEntity ButtonOBJ
	ButtonKeyOBJ = LoadMesh_Strict("GFX\map\ButtonKeycard.x")
	HideEntity ButtonKeyOBJ
	ButtonCodeOBJ = LoadMesh_Strict("GFX\map\ButtonCode.x")
	HideEntity ButtonCodeOBJ	
	ButtonScannerOBJ = LoadMesh_Strict("GFX\map\ButtonScanner.x")
	HideEntity ButtonScannerOBJ	
	
	BigDoorOBJ(0) = LoadMesh_Strict("GFX\map\ContDoorLeft.x")
	HideEntity BigDoorOBJ(0)
	BigDoorOBJ(1) = LoadMesh_Strict("GFX\map\ContDoorRight.x")
	HideEntity BigDoorOBJ(1)
	
	LeverBaseOBJ = LoadMesh_Strict("GFX\map\leverbase.x")
	HideEntity LeverBaseOBJ
	LeverOBJ = LoadMesh_Strict("GFX\map\leverhandle.x")
	HideEntity LeverOBJ
	
	For i = 0 To 1
		HideEntity BigDoorOBJ(i)
		If BumpEnabled And 0 Then 
			Local bumptex = LoadTexture_Strict("GFX\map\containmentdoorsbump.jpg")
			TextureBlend bumptex, FE_BUMP
			Local tex = LoadTexture_Strict("GFX\map\containment_doors.jpg")	
			EntityTexture BigDoorOBJ(i), bumptex, 0, 0
			EntityTexture BigDoorOBJ(i), tex, 0, 1
			FreeEntity tex
			FreeEntity bumptex
		EndIf
	Next
	
	DrawLoading(15)
	
	For i = 0 To 5
		GorePics(i) = LoadTexture_Strict("GFX\895pics\pic" + (i + 1) + ".jpg")
	Next
	
	OldAiPics(0) = LoadTexture_Strict("GFX\AIface.jpg")
	OldAiPics(1) = LoadTexture_Strict("GFX\AIface2.jpg")	
	
	DrawLoading(20)
	
	For i = 0 To 6
		DecalTextures(i) = LoadTexture_Strict("GFX\decal" + (i + 1) + ".png", 1 + 2)
	Next
	DecalTextures(7) = LoadTexture_Strict("GFX\items\INVpaperstrips.jpg", 1 + 2)
	For i = 8 To 12
		DecalTextures(i) = LoadTexture_Strict("GFX\decalpd"+(i-7)+".jpg", 1 + 2)	
	Next
	For i = 13 To 14
		DecalTextures(i) = LoadTexture_Strict("GFX\bullethole"+(i-12)+".jpg", 1 + 2)	
	Next	
	For i = 15 To 16
		DecalTextures(i) = LoadTexture_Strict("GFX\blooddrop"+(i-14)+".png", 1 + 2)	
	Next
	DecalTextures(17) = LoadTexture_Strict("GFX\decal8.png", 1 + 2)	
	DecalTextures(18) = LoadTexture_Strict("GFX\decalpd6.dc", 1 + 2)	
	DecalTextures(19) = LoadTexture_Strict("GFX\decal19.png", 1 + 2)
	
	DrawLoading(25)
	
	Monitor = LoadMesh_Strict("GFX\map\monitor.b3d")
	HideEntity Monitor
	MonitorTexture = LoadTexture_Strict("GFX\monitortexture.jpg")
	
	CamBaseOBJ = LoadMesh_Strict("GFX\map\cambase.x")
	HideEntity(CamBaseOBJ)
	CamOBJ = LoadMesh_Strict("GFX\map\CamHead.b3d")
	HideEntity(CamOBJ)
	
	InitItemTemplates()
	
	ParticleTextures(0) = LoadTexture_Strict("GFX\smoke.png", 1 + 2)
	ParticleTextures(1) = LoadTexture_Strict("GFX\flash.jpg", 1 + 2)
	ParticleTextures(2) = LoadTexture_Strict("GFX\dust.jpg", 1 + 2)
	ParticleTextures(3) = LoadTexture_Strict("GFX\npcs\hg.pt", 1 + 2)
	ParticleTextures(4) = LoadTexture_Strict("GFX\map\sun.jpg", 1 + 2)
	ParticleTextures(5) = LoadTexture_Strict("GFX\bloodsprite.png", 1 + 2)
	ParticleTextures(6) = LoadTexture_Strict("GFX\smoke2.png", 1 + 2)
	
	LoadMaterials("DATA\materials.ini")
	
	DrawLoading(30)
	
	;LoadRoomMeshes()
	
	;New Stuff for Loading in SCP:CB 1.3 - ENDSHN
	;DL_Init()
	;DL_SetReceiver(renderbrushes,RoomScale#,RoomScale#,RoomScale#)
	;DL_SetLight(Camera,40,0.5)
	
End Function

Function InitNewGame()
	
	Local i%, de.Decals, d.Doors, it.Items, r.Rooms, sc.SecurityCams 
	
	DrawLoading(45)
	
	HideDistance# = 15.0
	
	HeartBeatRate = 70
	
	AccessCode = 0
	For i = 0 To 3
		AccessCode = AccessCode + Rand(1,9)*(10^i)
	Next	
	
	If SelectedMap = "" Then
		CreateMap()
	Else
		LoadMap("Map Creator\Maps\"+SelectedMap)
	EndIf
	InitWayPoints()
	
	DrawLoading(79)
	
	Curr173 = CreateNPC(NPCtype173, 0, -30.0, 0)
	Curr106 = CreateNPC(NPCtypeOldMan, 0, -30.0, 0)
	Curr106\State = 70 * 60 * Rand(12,17)
	
	For d.Doors = Each Doors
		EntityParent(d\obj, 0)
		If d\obj2 > 0 Then EntityParent(d\obj2, 0)
		If d\frameobj > 0 Then EntityParent(d\frameobj, 0)
		If d\buttons[0] > 0 Then EntityParent(d\buttons[0], 0)
		If d\buttons[1] > 0 Then EntityParent(d\buttons[1], 0)
		
		If d\obj2 <> 0 And d\dir = 0 Then
			MoveEntity(d\obj, 0, 0, 8.0 * RoomScale)
			MoveEntity(d\obj2, 0, 0, 8.0 * RoomScale)
		EndIf	
	Next
	
	For it.Items = Each Items
		EntityType (it\obj, HIT_ITEM)
		EntityParent(it\obj, 0)
	Next
	
	DrawLoading(80)
	For sc.SecurityCams= Each SecurityCams
		sc\angle = EntityYaw(sc\obj) + sc\angle
		EntityParent(sc\obj, 0)
	Next	
	
	For r.Rooms = Each Rooms
		For i = 0 To 19
			If r\Lights[i]<>0 Then EntityParent(r\Lights[i],0)
		Next
		
		If (Not r\RoomTemplate\DisableDecals) Then
			If Rand(4) = 1 Then
				de.Decals = CreateDecal(Rand(2, 3), EntityX(r\obj)+Rnd(- 2,2), 0.003, EntityZ(r\obj)+Rnd(-2,2), 90, Rand(360), 0)
				de\Size = Rnd(0.1, 0.4) : ScaleSprite(de\obj, de\Size, de\Size)
				EntityAlpha(de\obj, Rnd(0.85, 0.95))
			EndIf
			
			If Rand(4) = 1 Then
				de.Decals = CreateDecal(0, EntityX(r\obj)+Rnd(- 2,2), 0.003, EntityZ(r\obj)+Rnd(-2,2), 90, Rand(360), 0)
				de\Size = Rnd(0.5, 0.7) : EntityAlpha(de\obj, 0.7) : de\ID = 1 : ScaleSprite(de\obj, de\Size, de\Size)
				EntityAlpha(de\obj, Rnd(0.7, 0.85))
			EndIf
		EndIf
		
		If (r\RoomTemplate\Name = "start" And IntroEnabled = False) Then 
			PositionEntity (Collider, EntityX(r\obj)+3584*RoomScale, 704*RoomScale, EntityZ(r\obj)+1024*RoomScale)
			PlayerRoom = r
		ElseIf (r\RoomTemplate\Name = "173" And IntroEnabled) Then
			PositionEntity (Collider, EntityX(r\obj), 1.0, EntityZ(r\obj))
			PlayerRoom = r
		EndIf
		
	Next
	
	Local rt.RoomTemplates
	For rt.RoomTemplates = Each RoomTemplates
		FreeEntity (rt\obj)
	Next	
	
	Local tw.TempWayPoints
	For tw.TempWayPoints = Each TempWayPoints
		Delete tw
	Next
	
	TurnEntity(Collider, 0, Rand(160, 200), 0)
	
	ResetEntity Collider
	
	If SelectedMap = "" Then InitEvents()
	
	MoveMouse viewport_center_x,viewport_center_y;320, 240
	
	SetFont Font1
	
	HidePointer()
	
	BlinkTimer = -10
	BlurTimer = 100
	Stamina = 100
	
	;DL_SetReceiver(BigRoomMesh)
	
	For i% = 0 To 70
		FPSfactor = 1.0
		FlushKeys()
		MovePlayer()
		UpdateDoors()
		UpdateNPCs()
		UpdateWorld()
		;Cls
		DrawLoading(80+Int(Float(i)*0.27))
	Next
	
	FreeTextureCache
	DrawLoading(100)
	
	FlushKeys
	FlushMouse
	
	DropSpeed = 0
	
	PrevTime = MilliSecs()
End Function

Function InitLoadGame()
	
	Local d.Doors, sc.SecurityCams, rt.RoomTemplates
	
	DrawLoading(80)
	
	For d.Doors = Each Doors
		EntityParent(d\obj, 0)
		If d\obj2 > 0 Then EntityParent(d\obj2, 0)
		If d\frameobj > 0 Then EntityParent(d\frameobj, 0)
		If d\buttons[0] > 0 Then EntityParent(d\buttons[0], 0)
		If d\buttons[1] > 0 Then EntityParent(d\buttons[1], 0)
		
	Next
	
	For sc.SecurityCams = Each SecurityCams
		sc\angle = EntityYaw(sc\obj) + sc\angle
		EntityParent(sc\obj, 0)
	Next
	
	ResetEntity Collider
	
	;InitEvents()
	
	DrawLoading(90)
	
	MoveMouse viewport_center_x,viewport_center_y
	
	SetFont Font1
	
	HidePointer ()
	
	BlinkTimer = BLINKFREQ
	Stamina = 100
	
	For rt.RoomTemplates = Each RoomTemplates
		If rt\obj <> 0 Then FreeEntity(rt\obj) : rt\obj = 0
	Next
	
	DropSpeed = 0.0
	
	FreeTextureCache
	
	DrawLoading(100)
	
	PrevTime = MilliSecs()
	FPSfactor = 0	
End Function

Function NullGame()
	Local i%, x%, y%, lvl
	Local itt.ItemTemplates, s.Screens, lt.LightTemplates, d.Doors, m.Materials
	Local wp.WayPoints, twp.TempWayPoints, r.Rooms, it.Items
	
	ClearTextureCache
	
	DeathMSG$=""
	
	SelectedMap = ""
	
	UsedConsole = False
	
	DoorTempID = 0
	RoomTempID = 0
	
	GameSaved = 0
	
	HideDistance# = 15.0
	
	CameraZoom Camera, 1.0
	
	For lvl = 0 To 0
		For x = 0 To MapWidth - 1
			For y = 0 To MapHeight - 1
				MapTemp(x, y) = 0
				MapFound(x, y) = 0
			Next
		Next
	Next
	
	For itt.ItemTemplates = Each ItemTemplates
		itt\found = False
	Next
	
	DropSpeed = 0
	Shake = 0
	CurrSpeed = 0
	
	DeathTimer=0
	
	HeartBeatVolume = 0
	
	StaminaEffect = 1.0
	StaminaEffectTimer = 0
	BlinkEffect = 1.0
	BlinkEffectTimer = 0
	
	Bloodloss = 0
	Injuries = 0
	Infect = 0
	
	For i = 0 To 5
		SCP1025state[i]=0
	Next
	
	SelectedEnding = ""
	EndingTimer = 0
	ExplosionTimer = 0
	
	CameraShake = 0
	Shake = 0
	LightFlash = 0
	
	GodMode = 0
	NoClip = 0
	WireframeState = 0
	WireFrame 0
	WearingGasMask = 0
	WearingHazmat = 0
	WearingVest = 0
	Wearing714 = 0
	Wearing178 = 0
	If WearingNightVision Then
		CameraFogFar = StoredCameraFogFar
		WearingNightVision = 0
	EndIf
	
	ForceMove = 0.0
	ForceAngle = 0.0	
	Playable = True
	
	Contained106 = False
	Disabled173 = False
	
	MTFtimer = 0
	For i = 0 To 9
		MTFrooms[i]=Null
		MTFroomState[i]=0
	Next
	
	For s.Screens = Each Screens
		If s\img <> 0 Then FreeImage s\img : s\img = 0
		Delete s
	Next
	
	For i = 0 To MAXACHIEVEMENTS-1
		Achievements(i)=0
	Next
	RefinedItems = 0
	
	ConsoleInput = ""
	ConsoleOpen = False
	
	EyeIrritation = 0
	EyeStuck = 0
	
	ShouldPlay = 0
	
	KillTimer = 0
	FallTimer = 0
	Stamina = 100
	BlurTimer = 0
	SuperMan = False
	SuperManTimer = 0
	
	InfiniteStamina% = False
	
	Msg = ""
	MsgTimer = 0
	
	SelectedItem = Null
	
	For i = 0 To MaxItemAmount - 1
		Inventory(i) = Null
	Next
	SelectedItem = Null
	
	ClosestButton = 0
	
	For d.Doors = Each Doors
		Delete d
	Next
	
	;ClearWorld
	
	For lt.LightTemplates = Each LightTemplates
		Delete lt
	Next 
	
	For m.Materials = Each Materials
		Delete m
	Next
	
	For wp.WayPoints = Each WayPoints
		Delete wp
	Next
	
	For twp.TempWayPoints = Each TempWayPoints
		Delete twp
	Next	
	
	For r.Rooms = Each Rooms
		Delete r
	Next
	
	For itt.ItemTemplates = Each ItemTemplates
		Delete itt
	Next 
	
	For it.Items = Each Items
		Delete it
	Next
	
	For pr.Props = Each Props
		Delete pr
	Next
	
	For de.decals = Each Decals
		Delete de
	Next
	
	For n.NPCS = Each NPCs
		Delete n
	Next
	Curr173 = Null
	Curr106 = Null
	Curr096 = Null
	For i = 0 To 6
		MTFrooms[i]=Null
	Next
	
	Local e.Events
	For e.Events = Each Events
		If e\Sound<>0 Then FreeSound_Strict e\Sound
		If e\Sound2<>0 Then FreeSound_Strict e\Sound2
		Delete e
	Next
	
	For sc.securitycams = Each SecurityCams
		Delete sc
	Next
	
	For em.emitters = Each Emitters
		Delete em
	Next	
	
	For p.particles = Each Particles
		Delete p
	Next
	
	For rt.RoomTemplates = Each RoomTemplates
		rt\obj = 0
	Next
	
	For i = 0 To 5
		If ChannelPlaying(RadioCHN(i)) Then StopChannel(RadioCHN(i))
	Next
	
	;Deleting all Stuff for SCP:CB 1.3 (new additional stuff) - ENDSHN
	;DL_Free()
	NTF_Wearing1499% = False
	NTF_1499PrevX# = 0.0
	NTF_1499PrevY# = 0.0
	NTF_1499PrevZ# = 0.0
	NTF_1499PrevRoom$ = ""
	NTF_1499X# = 0.0
	NTF_1499Y# = 0.0
	NTF_1499Z# = 0.0
	NTF_PrevPlayerRoom$ = ""
	
	DeInitExt
	
	ClearWorld
	
	InitExt
	
	For i=0 To 9
		If TempSounds[i]<>0 Then FreeSound_Strict TempSounds[i] : TempSounds[i]=0
	Next
	
End Function

Include "save.bb"

;--------------------------------------- music & sounds ----------------------------------------------

Function PlaySound2%(SoundHandle%, cam%, entity%, range# = 10, volume# = 1.0)
	range# = Max(range, 1.0)
	Local soundchn% = 0
	
	If volume > 0 Then 
		Local dist# = EntityDistance(cam, entity) / range#
		If 1 - dist# > 0 And 1 - dist# < 1
			Local panvalue# = Sin(-DeltaYaw(cam,entity))
			soundchn% = PlaySound_Strict (SoundHandle)
			
			ChannelVolume(soundchn, volume# * (1 - dist#))
			ChannelPan(soundchn, panvalue)			
		EndIf
	EndIf
	
	Return soundchn
End Function

Function LoopSound2%(SoundHandle%, Chn%, cam%, entity%, range# = 10, volume# = 1.0)
	range# = Max(range,1.0)
	
	If volume>0 Then
		
		Local dist# = EntityDistance(cam, entity) / range#
		If 1 - dist# > 0 And 1 - dist# < 1 Then
			
			Local panvalue# = Sin(-DeltaYaw(cam,entity))
			
			If Chn = 0 Then
				Chn% = PlaySound_Strict (SoundHandle)
			Else
				If (Not ChannelPlaying(Chn)) Then Chn% = PlaySound_Strict (SoundHandle)
			EndIf
			
			ChannelVolume(Chn, volume# * (1 - dist#))
			ChannelPan(Chn, panvalue)
		EndIf
	Else
		If Chn <> 0 Then
			ChannelVolume (Chn, 0)
		EndIf 
	EndIf
	
	Return Chn
End Function

Function LoadTempSound(file$)
	If TempSounds[TempSoundIndex]<>0 Then FreeSound_Strict(TempSounds[TempSoundIndex])
	TempSound = LoadSound_Strict(file)
	TempSounds[TempSoundIndex] = TempSound
	
	TempSoundIndex=(TempSoundIndex+1) Mod 10
	
	Return TempSound
End Function

Function LoadEventSound(e.Events,file$,num%=0)
	
	If num=0 Then
		If e\Sound<>0 Then FreeSound_Strict e\Sound : e\Sound=0
		e\Sound=LoadSound_Strict(file)
		Return e\Sound
	Else If num=1 Then
		If e\Sound2<>0 Then FreeSound_Strict e\Sound2 : e\Sound2=0
		e\Sound2=LoadSound_Strict(file)
		Return e\Sound2
	EndIf
End Function

Function UpdateMusic()
	
	If FPSfactor > 0 Then 
		If NowPlaying <> ShouldPlay Then ; playing the wrong clip, fade out
			CurrMusicVolume# = Max(CurrMusicVolume - (FPSfactor / 250.0), 0)
			If CurrMusicVolume = 0 Then
				NowPlaying = ShouldPlay
				If MusicCHN <> 0 Then StopChannel MusicCHN
			EndIf
		Else ; playing the right clip
			CurrMusicVolume = CurrMusicVolume + (MusicVolume - CurrMusicVolume) * 0.1
		EndIf
	EndIf
	
	If NowPlaying < 66 Then
		If MusicCHN = 0 Then
			MusicCHN = PlaySound_Strict(Music(NowPlaying))
		Else
			If (Not ChannelPlaying(MusicCHN)) Then MusicCHN = PlaySound_Strict(Music(NowPlaying))
		End If
	EndIf
	
	ChannelVolume MusicCHN, CurrMusicVolume
	
End Function 

Function PauseSounds()
	For e.events = Each Events
		If e\soundchn <> 0 Then
			If ChannelPlaying(e\soundchn) Then PauseChannel(e\soundchn)
		EndIf
		If e\soundchn2 <> 0 Then
			If ChannelPlaying(e\soundchn2) Then PauseChannel(e\soundchn2)
		EndIf		
	Next
	
	For n.npcs = Each NPCs
		If n\soundchn <> 0 Then
			If ChannelPlaying(n\soundchn) Then PauseChannel(n\soundchn)
		EndIf
	Next	
	
	For d.doors = Each Doors
		If d\soundchn <> 0 Then
			If ChannelPlaying(d\soundchn) Then PauseChannel(d\soundchn)
		EndIf
	Next	
	
	If AmbientSFXCHN <> 0 Then
		If ChannelPlaying(AmbientSFXCHN) Then PauseChannel(AmbientSFXCHN)
	EndIf
	
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then PauseChannel(BreathCHN)
	EndIf
End Function

Function ResumeSounds()
	For e.events = Each Events
		If e\soundchn <> 0 Then
			If ChannelPlaying(e\soundchn) Then ResumeChannel(e\soundchn)
		EndIf
		If e\soundchn2 <> 0 Then
			If ChannelPlaying(e\soundchn2) Then ResumeChannel(e\soundchn2)
		EndIf	
	Next
	
	For n.npcs = Each NPCs
		If n\soundchn <> 0 Then
			If ChannelPlaying(n\soundchn) Then ResumeChannel(n\soundchn)
		EndIf
	Next	
	
	For d.doors = Each Doors
		If d\soundchn <> 0 Then
			If ChannelPlaying(d\soundchn) Then ResumeChannel(d\soundchn)
		EndIf
	Next	
	
	If AmbientSFXCHN <> 0 Then
		If ChannelPlaying(AmbientSFXCHN) Then ResumeChannel(AmbientSFXCHN)
	EndIf	
	
	If BreathCHN <> 0 Then
		If ChannelPlaying(BreathCHN) Then ResumeChannel(BreathCHN)
	EndIf
End Function

Function GetStepSound()
	Local picker%,brush%,texture%,name$
	Local mat.Materials
	
	picker = LinePick(EntityX(Collider),EntityY(Collider),EntityZ(Collider),0,-1,0)
	If picker <> 0 Then
		brush = GetSurfaceBrush(GetSurface(picker,CountSurfaces(picker)))
		If brush<>0 Then
			texture = GetBrushTexture(brush,1)
			If texture <> 0 Then
				name = StripPath(TextureName(texture))
				If (name<>"") FreeTexture(texture)
				FreeBrush(brush)
				For mat.Materials = Each Materials
					If mat\name = name Then
						If mat\StepSound>0 Then
							Return mat\StepSound-1
						EndIf
						Exit
					EndIf
				Next				
			EndIf
		EndIf
	EndIf
	
	Return 0
End Function

;--------------------------------------- random -------------------------------------------------------

Function f2s$(n#, count%)
	Return Left(n, Len(Int(n))+count+1)
End Function

Function AnimateNPC(n.NPCs, start#, quit#, speed#, loop=True)
	Local newTime#
	
	If speed > 0.0 Then 
		newTime = Max(Min(n\Frame + speed * FPSfactor,quit),start)
		
		If loop And newTime => quit Then
			newTime = start
		EndIf
	Else
		If start < quit Then
			temp% = start
			start = quit
			quit = temp
		EndIf
		
		If loop Then
			newTime = n\Frame + speed * FPSfactor
			
			If newTime < quit Then 
				newTime = start
			Else If newTime > start 
				newTime = quit
			EndIf
		Else
			newTime = Max(Min(n\Frame + speed * FPSfactor,start),quit)
		EndIf
	EndIf
	
	SetNPCFrame(n, newTime)
End Function

Function SetNPCFrame(n.NPCs, frame#)
	If (Abs(n\Frame-frame)<0.001) Then Return
	
	SetAnimTime n\obj, frame
	
	n\Frame = frame
End Function

Function Animate2#(entity%, curr#, start%, quit%, speed#, loop=True)
	
	Local newTime#
	
	If speed > 0.0 Then 
		newTime = Max(Min(curr + speed * FPSfactor,quit),start)
		
		If loop Then
			If newTime => quit Then 
				;SetAnimTime entity, start
				newTime = start
			Else
				;SetAnimTime entity, newTime
			EndIf
		Else
			;SetAnimTime entity, newTime
		EndIf
	Else
		If start < quit Then
			temp% = start
			start = quit
			quit = temp
		EndIf
		
		If loop Then
			newTime = curr + speed * FPSfactor
			
			If newTime < quit Then newTime = start
			If newTime > start Then newTime = quit
			
			;SetAnimTime entity, newTime
		Else
			;SetAnimTime (entity, Max(Min(curr + speed * FPSfactor,start),quit))
			newTime = Max(Min(curr + speed * FPSfactor,start),quit)
		EndIf
	EndIf
	
	SetAnimTime entity, newTime
	Return newTime
	
End Function 


Function Use914(item.Items, setting$, x#, y#, z#)
	
	RefinedItems = RefinedItems+1
	
	Local it2.Items
	Select item\itemtemplate\name
		Case "Gas Mask", "Heavy Gas Mask"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case "1:1"
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
				Case "fine", "very fine"
					it2 = CreateItem("Gas Mask", "supergasmask", x, y, z)
					RemoveItem(item)
			End Select
		Case "Ballistic Vest"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case "1:1"
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
				Case "fine"
					it2 = CreateItem("Heavy Ballistic Vest", "finevest", x, y, z)
					RemoveItem(item)
				Case "very fine"
					it2 = CreateItem("Bulky Ballistic Vest", "veryfinevest", x, y, z)
					RemoveItem(item)
			End Select
		Case "3-D Glasses"
			Select setting
				Case "rough,coarse"
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
					For n.NPCs = Each NPCs
						If n\NPCtype = NPCtype178 Then RemoveNPC(n)
					Next
				Case "1:1","fine","very fine"
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
			End Select
		Case "Clipboard"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					For i% = 0 To 19
						If item\SecondInv[i]<>Null Then RemoveItem(item\SecondInv[i])
						item\SecondInv[i]=Null
					Next
					RemoveItem(item)
				Case "1:1"
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
				Case "fine"
					item\invSlots = Max(item\state2,15)
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
				Case "very fine"
					item\invSlots = Max(item\state2,20)
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
			End Select
		Case "Cowbell"
			Select setting
				Case "rough","coarse"
					d.Decals = CreateDecal(0, x, 8*RoomScale+0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case "1:1","fine","very fine"
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
			End Select
		Case "Night Vision Goggles"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
					RemoveItem(item)
				Case "1:1"
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)
				Case "fine", "very fine"
					it2 = CreateItem("Night Vision Goggles", "supernv", x, y, z)
					RemoveItem(item)
			End Select
		Case "Metal Panel", "SCP-148 Ingot"
			Select setting
				Case "rough", "coarse"
					it2 = CreateItem("SCP-148 Ingot", "scp148ingot", x, y, z)
					RemoveItem(item)
				Case "1:1", "fine", "very fine"
					it2 = Null
					For it.Items = Each Items
						If it<>item And it\obj <> 0 And it\Picked = False Then
							If Distance(EntityX(it\obj,True), EntityZ(it\obj,True), EntityX(item\obj, True), EntityZ(item\obj, True)) < (180.0 * RoomScale) Then
								it2 = it
								Exit
							ElseIf Distance(EntityX(it\obj,True), EntityZ(it\obj,True), x,z) < (180.0 * RoomScale)
								it2 = it
								Exit
							End If
						End If
					Next
					
					If it2<>Null Then
						Select it2\itemtemplate\tempname
							Case "gasmask", "supergasmask"
								RemoveItem (it2)
								RemoveItem (item)
								
								it2 = CreateItem("Heavy Gas Mask", "gasmask3", x, y, z)
							Case "vest"
								RemoveItem (it2)
								RemoveItem(item)
								it2 = CreateItem("Heavy Ballistic Vest", "finevest", x, y, z)
							Case "hazmatsuit","hazmatsuit2"
								RemoveItem (it2)
								RemoveItem(item)
								it2 = CreateItem("Heavy Hazmat Suit", "hazmatsuit3", x, y, z)
						End Select
					Else 
						If item\itemtemplate\name="SCP-148 Ingot" Then
							it2 = CreateItem("Metal Panel", "scp148", x, y, z)
							RemoveItem(item)
						Else
							PositionEntity(item\obj, x, y, z)
							ResetEntity(item\obj)							
						EndIf
					EndIf					
			End Select
			
		Case "Severed Hand"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(3, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1","fine","very fine"
					it2 = CreateItem("Severed Hand", "hand2", x, y, z)
			End Select
			RemoveItem(item)
		Case "First Aid Kit"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					it2 = CreateItem("Blue First Aid Kit", "firstaid2", x, y, z)
				Case "fine"
					it2 = CreateItem("Small First Aid Kit", "finefirstaid", x, y, z)
				Case "very fine"
					it2 = CreateItem("Strange Bottle", "veryfinefirstaid", x, y, z)
			End Select
			RemoveItem(item)
		Case "Level 1 Key Card", "Level 2 Key Card", "Level 3 Key Card", "Level 4 Key Card", "Level 5 Key Card", "Key Card"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					it2 = CreateItem("Playing Card", "misc", x, y, z)
				Case "fine"
					If Rand(6)=1 Then 
						it2 = CreateItem("Playing Card", "misc", x, y, z)
					Else
						Select item\itemtemplate\name
							Case "Level 1 Key Card"
								it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)
							Case "Level 2 Key Card"
								it2 = CreateItem("Level 3 Key Card", "key3", x, y, z)
							Case "Level 3 Key Card"
								it2 = CreateItem("Mastercard", "misc", x, y, z)
							Case "Level 4 Key Card"
								it2 = CreateItem("Level 5 Key Card", "key5", x, y, z)
							Case "Level 5 Key Card"	
								it2 = CreateItem("Key Card Omni", "key6", x, y, z)
						End Select						
					EndIf
				Case "very fine"
					If Rand(3)=1 Then
						it2 = CreateItem("Key Card Omni", "key6", x, y, z)
					Else	
						it2 = CreateItem("Mastercard", "misc", x, y, z)
					EndIf
			End Select			
			
			RemoveItem(item)
		Case "Key Card Omni"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					If Rand(2)=1 Then
						it2 = CreateItem("Mastercard", "misc", x, y, z)
					Else
						it2 = CreateItem("Playing Card", "misc", x, y, z)			
					EndIf	
				Case "fine", "very fine"
					it2 = CreateItem("Key Card Omni", "key6", x, y, z)
			End Select			
			
			RemoveItem(item)
		Case "Playing Card", "Mastercard"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
					d\Size = 0.07 : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1", "fine", "very fine"
					If Rand(2)=1 Then
						it2 = CreateItem("Mastercard", "misc", x, y, z)				
					Else
						it2 = CreateItem("Level 2 Key Card", "key2", x, y, z)	
					EndIf
			End Select
			RemoveItem(item)
		Case "S-NAV 300 Navigator", "S-NAV 310 Navigator", "S-NAV Navigator", "S-NAV Navigator Ultimate"
			Select setting
				Case "rough", "coarse"
					it2 = CreateItem("Electronical components", "misc", x, y, z)
				Case "1:1"
					it2 = CreateItem("S-NAV Navigator", "nav", x, y, z)
					it2\state = 100
				Case "fine"
					it2 = CreateItem("S-NAV 310 Navigator", "nav", x, y, z)
					it2\state = 100
				Case "very fine"
					it2 = CreateItem("S-NAV Navigator Ultimate", "nav", x, y, z)
					it2\state = 101
			End Select
			
			RemoveItem(item)
		Case "Radio Transceiver"
			Select setting
				Case "rough", "coarse"
					it2 = CreateItem("Electronical components", "misc", x, y, z)
				Case "1:1"
					it2 = CreateItem("Radio Transceiver", "18vradio", x, y, z)
					it2\state = 100
				Case "fine"
					it2 = CreateItem("Radio Transceiver", "fineradio", x, y, z)
					it2\state = 101
				Case "very fine"
					it2 = CreateItem("Radio Transceiver", "veryfineradio", x, y, z)
					it2\state = 101
			End Select
			
			RemoveItem(item)
		Case "SCP-513"
			Select setting
				Case "rough", "coarse"
					PlaySound_Strict LoadTempSound("SFX\Bell4.ogg")
					For n.npcs = Each NPCs
						If n\npctype = NPCtype5131 Then RemoveNPC(n)
					Next
					d.Decals = CreateDecal(0, x, 8*RoomScale+0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					
				Case "fine"
					
				Case "very fine"
					
			End Select
			
			RemoveItem(item)
		Case "Some SCP-420-J", "Cigarette"
			Select setting
				Case "rough", "coarse"			
					d.Decals = CreateDecal(0, x, 8*RoomScale+0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					it2 = CreateItem("Cigarette", "cigarette", x + 1.5, y + 0.5, z + 1.0)
				Case "fine"
					it2 = CreateItem("Joint", "420s", x + 1.5, y + 0.5, z + 1.0)
				Case "very fine"
					it2 = CreateItem("Smelly Joint", "420s", x + 1.5, y + 0.5, z + 1.0)
			End Select
			
			RemoveItem(item)
		Case "9V Battery", "18V Battery", "Strange Battery"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					it2 = CreateItem("18V Battery", "18vbat", x, y, z)
				Case "fine"
					it2 = CreateItem("Strange Battery", "killbat", x, y, z)
				Case "very fine"
					it2 = CreateItem("Strange Battery", "killbat", x, y, z)
			End Select
			
			RemoveItem(item)
		Case "ReVision Eyedrops", "RedVision Eyedrops", "Eyedrops"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					it2 = CreateItem("RedVision Eyedrops", "eyedrops", x,y,z)
				Case "fine"
					it2 = CreateItem("Eyedrops", "fineeyedrops", x,y,z)
				Case "very fine"
					it2 = CreateItem("Eyedrops", "supereyedrops", x,y,z)
			End Select
			
			RemoveItem(item)		
		Case "Hazmat Suit"
			Select setting
				Case "rough", "coarse"
					d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
					d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
				Case "1:1"
					it2 = CreateItem("Hazmat Suit", "hazmatsuit", x,y,z)
				Case "fine"
					it2 = CreateItem("Hazmat Suit", "hazmatsuit2", x,y,z)
				Case "very fine"
					it2 = CreateItem("Hazmat Suit", "hazmatsuit2", x,y,z)
			End Select
			
			RemoveItem(item)
		Default
			
			Select item\itemtemplate\tempname
				Case "cup"
					Select setting
						Case "rough", "coarse"
							d.Decals = CreateDecal(0, x, 8 * RoomScale + 0.010, z, 90, Rand(360), 0)
							d\Size = 0.2 : EntityAlpha(d\obj, 0.8) : ScaleSprite(d\obj, d\Size, d\Size)
						Case "1:1"
							it2 = CreateItem("cup", "cup", x,y,z)
							it2\name = item\name
							it2\r = 255-item\r
							it2\g = 255-item\g
							it2\b = 255-item\b
						Case "fine"
							it2 = CreateItem("cup", "cup", x,y,z)
							it2\name = item\name
							it2\state = 1.0
							it2\r = Min(item\r*Rnd(0.9,1.1),255)
							it2\g = Min(item\g*Rnd(0.9,1.1),255)
							it2\b = Min(item\b*Rnd(0.9,1.1),255)
						Case "very fine"
							it2 = CreateItem("cup", "cup", x,y,z)
							it2\name = item\name
							it2\state = Max(it2\state*2.0,2.0)	
							it2\r = Min(item\r*Rnd(0.5,1.5),255)
							it2\g = Min(item\g*Rnd(0.5,1.5),255)
							it2\b = Min(item\b*Rnd(0.5,1.5),255)
							If Rand(5)=1 Then
								ExplosionTimer = 135
							EndIf
					End Select	
					
					RemoveItem(item)
				Case "paper"
					Select setting
						Case "rough", "coarse"
							d.Decals = CreateDecal(7, x, 8 * RoomScale + 0.005, z, 90, Rand(360), 0)
							d\Size = 0.12 : ScaleSprite(d\obj, d\Size, d\Size)
						Case "1:1"
							Select Rand(6)
								Case 1
									it2 = CreateItem("Document SCP-106", "paper", x, y, z)
								Case 2
									it2 = CreateItem("Document SCP-079", "paper", x, y, z)
								Case 3
									it2 = CreateItem("Document SCP-173", "paper", x, y, z)
								Case 4
									it2 = CreateItem("Document SCP-895", "paper", x, y, z)
								Case 5
									it2 = CreateItem("Document SCP-682", "paper", x, y, z)
								Case 6
									it2 = CreateItem("Document SCP-860", "paper", x, y, z)
							End Select
						Case "fine", "very fine"
							it2 = CreateItem("Origami", "misc", x, y, z)
					End Select
					
					RemoveItem(item)
				Default
					PositionEntity(item\obj, x, y, z)
					ResetEntity(item\obj)	
			End Select
			
	End Select
	
	If it2 <> Null Then EntityType (it2\obj, HIT_ITEM)
End Function

Function Use294()
	Local x#,y#, xtemp%,ytemp%, strtemp$, temp%
	
	ShowPointer()
	
	x = GraphicWidth/2 - (ImageWidth(Panel294)/2)
	y = GraphicHeight/2 - (ImageHeight(Panel294)/2)
	DrawImage Panel294, x, y
	If Fullscreen Then DrawImage CursorIMG, MouseX(),MouseY()
	
	temp = True
	If PlayerRoom\SoundCHN<>0 Then temp = False
	
	Text x+907, y+185, Input294, True,True
	
	If temp Then
		If MouseHit1 Then
			xtemp = Floor((MouseX()-x-228) / 35.5)
			ytemp = Floor((MouseY()-y-342) / 36.5)
			
			If ytemp => 0 And ytemp < 5 Then
				If xtemp => 0 And xtemp < 10 Then PlaySound_Strict ButtonSFX
			EndIf
			
			strtemp = ""
			
			temp = False
			
			Select ytemp
				Case 0
					strtemp = (xtemp + 1) Mod 10
				Case 1
					Select xtemp
						Case 0
							strtemp = "Q"
						Case 1
							strtemp = "W"
						Case 2
							strtemp = "E"
						Case 3
							strtemp = "R"
						Case 4
							strtemp = "T"
						Case 5
							strtemp = "Y"
						Case 6
							strtemp = "U"
						Case 7
							strtemp = "I"
						Case 8
							strtemp = "O"
						Case 9
							strtemp = "P"
					End Select
				Case 2
					Select xtemp
						Case 0
							strtemp = "A"
						Case 1
							strtemp = "S"
						Case 2
							strtemp = "D"
						Case 3
							strtemp = "F"
						Case 4
							strtemp = "G"
						Case 5
							strtemp = "H"
						Case 6
							strtemp = "J"
						Case 7
							strtemp = "K"
						Case 8
							strtemp = "L"
						Case 9 ;dispense
							temp = True
					End Select
				Case 3
					Select xtemp
						Case 0
							strtemp = "Z"
						Case 1
							strtemp = "X"
						Case 2
							strtemp = "C"
						Case 3
							strtemp = "V"
						Case 4
							strtemp = "B"
						Case 5
							strtemp = "N"
						Case 6
							strtemp = "M"
						Case 7
							strtemp = "-"
						Case 8
							strtemp = " "
						Case 9
							Input294 = Left(Input294, Max(Len(Input294)-1,0))
					End Select
				Case 4
					strtemp = " "
			End Select
			
			Input294 = Input294 + strtemp
			
			Input294 = Left(Input294, Min(Len(Input294),15))
			
			If temp And Input294<>"" Then ;dispense
				Input294 = Trim(Lower(Input294))
				If Left(Input294, Min(7,Len(Input294))) = "cup of " Then
					Input294 = Right(Input294, Len(Input294)-7)
				ElseIf Left(Input294, Min(9,Len(Input294))) = "a cup of " 
					Input294 = Right(Input294, Len(Input294)-9)
				EndIf
				
				Local loc% = GetINISectionLocation("DATA\SCP-294.ini", Input294)
				
				If loc > 0 Then
					strtemp$ = GetINIString2("DATA\SCP-294.ini", loc, "dispensesound")
					If strtemp="" Then
						PlayerRoom\SoundCHN = PlaySound_Strict (LoadTempSound("SFX\294\dispense1.ogg"))
					Else
						PlayerRoom\SoundCHN = PlaySound_Strict (LoadTempSound(strtemp))
					EndIf
					
					If GetINIInt2("DATA\SCP-294.ini", loc, "explosion")=True Then 
						ExplosionTimer = 135
						DeathMSG = GetINIString2("DATA\SCP-294.ini", loc, "deathmessage")
					EndIf
					
					strtemp$ = GetINIString2("DATA\SCP-294.ini", loc, "color")
					
					sep1 = Instr(strtemp, ",", 1)
					sep2 = Instr(strtemp, ",", sep1+1)
					r% = Trim(Left(strtemp, sep1-1))
					g% = Trim(Mid(strtemp, sep1+1, sep2-sep1-1))
					b% = Trim(Right(strtemp, Len(strtemp)-sep2))
					
					alpha# = Float(GetINIString2("DATA\SCP-294.ini", loc, "alpha"))
					glow = GetINIInt2("DATA\SCP-294.ini", loc, "glow")
					If alpha = 0 Then alpha = 1.0
					If glow Then alpha = -alpha
					
					it.items = CreateItem("Cup", "cup", EntityX(PlayerRoom\Objects[1],True),EntityY(PlayerRoom\Objects[1],True),EntityZ(PlayerRoom\Objects[1],True), r,g,b,alpha)
					it\name = "Cup of "+Input294
					EntityType (it\obj, HIT_ITEM)
					
				Else
					;out of range
					Input294 = "OUT OF RANGE"
					PlayerRoom\SoundCHN = PlaySound_Strict (LoadTempSound("SFX\294\outofrange.ogg"))
				EndIf
				
			EndIf
			
		EndIf ;if mousehit1
		
		If MouseHit2 Or (Not Using294) Then 
			HidePointer()
			Using294 = False
			Input294 = ""
		EndIf
		
	Else ;playing a dispensing sound
		If Input294 <> "OUT OF RANGE" Then Input294 = "DISPENSING..."
		
		If Not ChannelPlaying(PlayerRoom\SoundCHN) Then
			If Input294 <> "OUT OF RANGE" Then
				HidePointer()
				Using294 = False
			EndIf
			Input294=""
			PlayerRoom\SoundCHN=0
		EndIf
	EndIf
	
End Function



Function UpdateMTF%()
	If PlayerRoom\RoomTemplate\Name = "gateaentrance" Then Return
	
	Local r.Rooms, n.NPCs
	Local dist#, i%
	
	;mtf ei viel‰ spawnannut, spawnataan jos pelaaja menee tarpeeksi l‰helle gate b:t‰
	If MTFtimer = 0 Then
		If Rand(30)=1 Then
			
			Local entrance.Rooms = Null
			For r.Rooms = Each Rooms
				If Lower(r\RoomTemplate\Name) = "gateaentrance" Then entrance = r : Exit
			Next
			
			If entrance <> Null Then 
				If Abs(EntityZ(entrance\obj)-EntityZ(Collider))<30.0 Then
					If PlayerRoom\RoomTemplate\Name<>"room860" And PlayerRoom\RoomTemplate\Name<>"pocketdimension" Then
						PlaySound_Strict LoadTempSound("SFX\MTF\Announc.ogg")
					EndIf
					
					MTFtimer = 1
					Local leader.NPCs
					For i = 0 To 2
						n.NPCs = CreateNPC(NPCtypeMTF, EntityX(entrance\obj)+0.3*(i-1), 1.0,EntityZ(entrance\obj)+8.0)
						
						If i = 0 Then 
							leader = n
						Else
							n\MTFLeader = leader
						EndIf
						
						n\PrevState = 0
						n\PrevX = i
					Next
				EndIf
			EndIf
		EndIf
	Else
		Return
		
		;mtf spawnannut, aletaan p‰ivitt‰‰ teko‰ly‰
		
		MTFtimer=MTFtimer+FPSfactor
		
		;mtfroomstate 0 = huonetta ei ole alettu viel‰ etsi‰
		;mtfroomstate 1 = joku tiimi on menossa huoneeseen
		;mtfroomstate 2 = huone on tarkistettu
		;mtfroomstate 3 = huoneeseen ei lˆydetty reitti‰ -> yritet‰‰n v‰h‰n ajan p‰‰st‰ uudestaan
		
		;prevstate 0 = k‰y l‰pi tutkimattomia huoneita
		
		;p‰ivitet‰‰n kymmenen sekunnin v‰lein MTF:n "kollektiivinen teko‰ly"
		If MTFtimer > (70*10) Then
			
			DebugLog "MTF update"
			
			;tiimi saapunut 106:n huoneeseen, "pyydystet‰‰n" se
			If MTFrooms[0]<>Null Then
				If MTFroomState[0]=2 Then
					If PlayerRoom\RoomTemplate\Name<>"room106" Then
						If Contained106 Then
							PlayMTFSound(LoadTempSound("SFX\MTF\Oldman2.ogg"),Null)
							MTFroomState[0]=4
						ElseIf Curr106\State>0 
							PlayMTFSound(LoadTempSound("SFX\MTF\Oldman1.ogg"),Null)
							Contained106=True
							MTFroomState[0]=4
						EndIf
					EndIf
				EndIf
			EndIf
			
			For i = 0 To 6
				If MTFroomState[i]=1 Then MTFroomState[i] = 0
				
				If MTFroomState[i]=3 Then
					DebugLog "ei reitti‰ ("+MTFrooms[i]\RoomTemplate\Name+"), ohitetaan"
					If Rand(8)=1 Then MTFroomState[i] = 0
				EndIf		
			Next
			
			For n.NPCs = Each NPCs
				If n\NPCtype = NPCtypeMTF And n\PrevX = 0 And n\LastSeen =< 0 And n\Target = Null And n\PathStatus <> 1 Then
					;etsit‰‰n reitti l‰himp‰‰n huoneeseen jota ei ole viet‰ k‰yty tutkimassa
					Local targetRoom%, targetRoomDist#=500.0
					For i = 0 To 6
						If MTFrooms[i]<>Null Then
							If MTFroomState[i] = 0 Then 
								dist# = EntityDistance(n\Collider, MTFrooms[i]\obj)
								If dist < targetRoomDist Then
									targetRoomDist = dist
									targetRoom = i
								EndIf
							EndIf
						EndIf
					Next
					
					If targetRoomDist < 500.0 Then
						If Distance(EntityX(MTFrooms[targetRoom]\obj,True),EntityZ(MTFrooms[targetRoom]\obj,True),EntityX(n\Collider),EntityZ(n\Collider))< 8.0 Then
							;tiimi saapunut huoneeseen, merkataan ett‰ se on tarkistettu
							MTFroomState[targetRoom]=2
							
							Select MTFrooms[targetRoom]\RoomTemplate\Name 
								Case "room106"
									PlayMTFSound(LoadTempSound("SFX\MTF\Oldman0.ogg"),n)
									
									n\PathStatus = FindPath(n, EntityX(MTFrooms[targetRoom]\Objects[9],True),EntityY(MTFrooms[targetRoom]\Objects[9],True),EntityZ(MTFrooms[targetRoom]\Objects[9],True))
									n\PathTimer = 70*30
									n\State=3
								Case "start"
									If (Curr173\Idle<3) Then
										PlayMTFSound(LoadTempSound("SFX\MTF\173cont"+Rand(1,4)+".ogg"),n)
									EndIf
									
									PositionEntity Curr173\Collider, EntityX(r\obj,True)+4736*RoomScale,450*RoomScale,EntityZ(r\obj,True)+1692*RoomScale
									Curr173\Idle = 3
								Default
									For n2.npcs = Each NPCs
										If n2 <> n And n2\PrevState = n\PrevState And n2\NPCtype = NPCtypeMTF Then
											n2\state = 0
										EndIf
									Next															
							End Select
						Else
							
							Local currentRoom.Rooms, currentRoomDist#
							Local closestRoom.Rooms, closestRoomDist# = 500								
							If targetRoomDist < 16.0 Then 
								closestRoom = MTFrooms[targetRoom]
							Else
								
								For r.Rooms = Each Rooms
									If Abs(EntityX(n\Collider)-EntityX(r\obj))<4.0 Then
										If Abs(EntityZ(n\Collider)-EntityZ(r\obj))<4.0 Then
											currentRoom = r
											currentRoomDist = EntityDistance(r\obj, MTFrooms[targetRoom]\obj)
											Exit
										EndIf
									EndIf
								Next
								
								If currentRoom <> Null Then
									For r.Rooms = Each Rooms
										If r<>MTFrooms[targetRoom] Then
											If EntityDistance(r\obj, MTFrooms[targetRoom]\obj)<currentRoomDist Then
												dist = EntityDistance(r\obj, currentRoom\obj)
												If dist < closestRoomDist Then
													closestRoom = r
													closestRoomDist = dist
												EndIf
											EndIf
										EndIf
									Next										
								EndIf									
							EndIf
							
							If closestRoom <> Null Then
								If EntityDistance(Collider, n\Collider)<HideDistance Then
									n\PathStatus = FindPath(n, EntityX(closestRoom\obj,True)+Rnd(-0.3,0.3), 0.4, EntityZ(closestRoom\obj,True)+Rnd(-0.3,0.3))
									
									If n\PathStatus = 2 Then 
										;MTFroomState[targetRoom]=3
									ElseIf n\PathStatus = 1
										;For n2.npcs = Each NPCs
										;	If n2 <> n And n2\NPCtype = NPCtypeMTF And n2\State = 0 Then
										;		n2\state = 4
										;		n2\target = n
										;	EndIf
										;Next
										MTFroomState[targetRoom]=1 
										n\State = 3
									EndIf		
									
								Else
									PositionEntity n\Collider, EntityX(closestRoom\obj), 0.5, EntityZ(closestRoom\obj)
									ResetEntity n\Collider
									
									For n2.npcs = Each NPCs
										If n2 <> n And n2\NPCtype = NPCtypeMTF Then
											If EntityDistance(n2\collider, Collider)>HideDistance Then
												PositionEntity n2\Collider, EntityX(closestRoom\obj)+Rnd(-0.2,0.2), 0.5, EntityZ(closestRoom\obj)+Rnd(-0.2,0.2)
												ResetEntity n2\Collider
											EndIf
										EndIf
									Next										
								EndIf
							EndIf
							
							
						EndIf
					EndIf
						
					Exit
				EndIf
			Next
			
			MTFtimer = 1.0
		EndIf
		
	EndIf
End Function


Function UpdateInfect()
	Local temp#, i%, r.Rooms
	
	If Infect>0 Then
		ShowEntity InfectOverlay
		
		If Infect < 93.0 Then
			temp=Infect
			Infect = Min(Infect+FPSfactor*0.002,100)
			
			BlurTimer = Max(Infect*3*(2.0-CrouchState),BlurTimer)
			
			HeartBeatRate = Max(HeartBeatRate, 100)
			HeartBeatVolume = Max(HeartBeatVolume, Infect/120.0)
			
			EntityAlpha InfectOverlay, Min(((Infect*0.2)^2)/1000.0,0.5) * (Sin(MilliSecs()/8.0)+2.0)
			
			For i = 0 To 5
				If Infect>i*15+10 And temp =< i*15+10 Then
					PlaySound_Strict LoadTempSound("SFX\008voices"+i+".ogg")
				EndIf
			Next
			
			If Infect > 40 And temp =< 40.0 Then
				Msg = "You feel like feverish."
				MsgTimer = 70*6
			ElseIf Infect > 80 And temp =< 80.0
				Msg = "You feel very faint..."
				MsgTimer = 70*4
			ElseIf Infect =>91.5
				BlinkTimer = Max(Min(-10*(Infect-91.5),BlinkTimer),-10)
				If Infect >= 92.7 And temp < 92.7 Then
					For r.Rooms = Each Rooms
						If r\RoomTemplate\Name="008" Then
							PositionEntity Collider, EntityX(r\Objects[7],True),EntityY(r\Objects[7],True),EntityZ(r\Objects[7],True),True
							ResetEntity Collider
							r\NPC[0] = CreateNPC(NPCtypeD, EntityX(r\Objects[6],True),EntityY(r\Objects[6],True)+0.2,EntityZ(r\Objects[6],True))
							r\NPC[0]\Sound = LoadSound_Strict("SFX\008death1.ogg")
							r\NPC[0]\SoundChn = PlaySound_Strict(r\NPC[0]\Sound)
							tex = LoadTexture_Strict("GFX\npcs\scientist2.jpg")
							EntityTexture r\NPC[0]\obj, tex
							FreeTexture tex
							r\NPC[0]\State=6
							PlayerRoom = r
							Exit
						EndIf
					Next
				EndIf
			EndIf
		Else
			temp=Infect
			Infect = Min(Infect+FPSfactor*0.004,100)
			
			If Infect < 94.7 Then
				EntityAlpha InfectOverlay, 0.5 * (Sin(MilliSecs()/8.0)+2.0)
				BlurTimer = 900
				
				If Infect > 94.5 Then BlinkTimer = Max(Min(-50*(Infect-94.5),BlinkTimer),-10)
				PointEntity Collider, PlayerRoom\NPC[0]\Collider
				PointEntity PlayerRoom\NPC[0]\Collider, Collider
				ForceMove = 0.4
				Injuries = 2.5
				Bloodloss = 0
				
				Animate2(PlayerRoom\NPC[0]\obj, AnimTime(PlayerRoom\NPC[0]\obj), 357, 381, 0.3)
			ElseIf Infect < 98.5
				
				EntityAlpha InfectOverlay, 0.5 * (Sin(MilliSecs()/5.0)+2.0)
				BlurTimer = 950
				
				If temp < 94.7 Then 
					PlayerRoom\NPC[0]\Sound = LoadSound_Strict("SFX\008death2.ogg")
					PlayerRoom\NPC[0]\SoundChn = PlaySound_Strict(PlayerRoom\NPC[0]\Sound)
					
					DeathMSG = "Subject D-9341 found ingesting Dr. [REDACTED] at Sector [REDACTED]. Subject immediately terminated by Nine Tailed Fox and sent for autopsy. "
					DeathMSG = DeathMSG + "SCP-008 infection was confirmed, after which the body was incinerated."
					
					Kill()
					de.Decals = CreateDecal(3, EntityX(PlayerRoom\NPC[0]\Collider), 544*RoomScale + 0.01, EntityZ(PlayerRoom\NPC[0]\Collider),90,Rnd(360),0)
					de\Size = 0.8
					ScaleSprite(de\obj, de\Size,de\Size)
				ElseIf Infect > 96
					BlinkTimer = Max(Min(-10*(Infect-96),BlinkTimer),-10)
				Else
					KillTimer = Max(-350, KillTimer)
				EndIf
				
				If PlayerRoom\NPC[0]\State2=0 Then
					Animate2(PlayerRoom\NPC[0]\obj, AnimTime(PlayerRoom\NPC[0]\obj), 13, 19, 0.3,False)
					If AnimTime(PlayerRoom\NPC[0]\obj) => 19 Then PlayerRoom\NPC[0]\State2=1
				Else
					Animate2(PlayerRoom\NPC[0]\obj, AnimTime(PlayerRoom\NPC[0]\obj), 19, 13, -0.3)
					If AnimTime(PlayerRoom\NPC[0]\obj) =< 13 Then PlayerRoom\NPC[0]\State2=0
				EndIf
				
				If Rand(50)=1 Then
					p.Particles = CreateParticle(EntityX(PlayerRoom\NPC[0]\Collider),EntityY(PlayerRoom\NPC[0]\Collider),EntityZ(PlayerRoom\NPC[0]\Collider), 5, Rnd(0.05,0.1), 0.15, 200)
					p\speed = 0.01
					p\SizeChange = 0.01
					p\A = 0.5
					p\Achange = -0.01
					RotateEntity p\pvt, Rnd(360),Rnd(360),0
				EndIf
				
				PositionEntity Head, EntityX(PlayerRoom\NPC[0]\Collider,True), EntityY(PlayerRoom\NPC[0]\Collider,True)+0.65,EntityZ(PlayerRoom\NPC[0]\Collider,True),True
				RotateEntity Head, (1.0+Sin(MilliSecs()/5.0))*15, PlayerRoom\angle-180, 0, True
				MoveEntity Head, 0,0,0.4
				TurnEntity Head, 80+(Sin(MilliSecs()/5.0))*30,(Sin(MilliSecs()/5.0))*40,0
			EndIf
		EndIf
		
		
	Else
		HideEntity InfectOverlay
	EndIf
End Function

;--------------------------------------- math -------------------------------------------------------

Function Distance#(x1#, y1#, x2#, y2#)
	Local x# = x2 - x1, y# = y2 - y1
	Return(Sqr(x*x + y*y))
End Function


Function CurveValue#(number#, old#, smooth#)
	If FPSfactor = 0 Then Return old
	
	If number < old Then
		Return Max(old + (number - old) * (1.0 / smooth * FPSfactor), number)
	Else
		Return Min(old + (number - old) * (1.0 / smooth * FPSfactor), number)
	EndIf
End Function

Function CurveAngle#(val#, old#, smooth#)
	If FPSfactor = 0 Then Return old
	
   Local diff# = WrapAngle(val) - WrapAngle(old)
   If diff > 180 Then diff = diff - 360
   If diff < - 180 Then diff = diff + 360
   Return WrapAngle(old + diff * (1.0 / smooth * FPSfactor))
End Function




Function WrapAngle#(angle#)
	If angle = INFINITY Then Return 0.0
	While angle < 0
		angle = angle + 360
	Wend 
	While angle >= 360
		angle = angle - 360
	Wend
	Return angle
End Function

Function GetAngle#(x1#, y1#, x2#, y2#)
	Return ATan2( y2 - y1, x2 - x1 )
End Function

Function CircleToLineSegIsect% (cx#, cy#, r#, l1x#, l1y#, l2x#, l2y#)
	
	;Palauttaa:
	;  True (1) kun:
	;      Ympyr‰ [keskipiste = (cx, cy): s‰de = r]
	;      leikkaa janan, joka kulkee pisteiden (l1x, l1y) & (l2x, l2y) kaitta
	;  False (0) muulloin
	
	;Ympyr‰n keskipisteen ja (ainakin toisen) janan p‰‰tepisteen et‰isyys < r
	;-> leikkaus
	If Distance(cx, cy, l1x, l1y) <= r Then
		Return True
	EndIf
	
	If Distance(cx, cy, l2x, l2y) <= r Then
		Return True
	EndIf	
	
	;Vektorit (janan vektori ja vektorit janan p‰‰tepisteist‰ ympyr‰n keskipisteeseen)
	Local SegVecX# = l2x - l1x
	Local SegVecY# = l2y - l1y
	
	Local PntVec1X# = cx - l1x
	Local PntVec1Y# = cy - l1y
	
	Local PntVec2X# = cx - l2x
	Local PntVec2Y# = cy - l2y
	
	;Em. vektorien pistetulot
	Local dp1# = SegVecX * PntVec1X + SegVecY * PntVec1Y
	Local dp2# = -SegVecX * PntVec2X - SegVecY * PntVec2Y
	
	;Tarkistaa onko toisen pistetulon arvo 0
	;tai molempien merkki sama
	If dp1 = 0 Or dp2 = 0 Then
	ElseIf (dp1 > 0 And dp2 > 0) Or (dp1 < 0 And dp2 < 0) Then
	Else
		;Ei kumpikaan -> ei leikkausta
		Return False
	EndIf
	
	;Janan p‰‰tepisteiden kautta kulkevan suoran ;yht‰lˆ; (ax + by + c = 0)
	Local a# = (l2y - l1y) / (l2x - l1x)
	Local b# = -1
	Local c# = -(l2y - l1y) / (l2x - l1x) * l1x + l1y
	
	;Ympyr‰n keskipisteen et‰isyys suorasta
	Local d# = Abs(a * cx + b * cy + c) / Sqr(a * a + b * b)
	
	;Ympyr‰ on liian kaukana
	;-> ei leikkausta
	If d > r Then Return False
	
	;Local kateetin_pituus# = Cos(angle) * hyp
	
	;Jos p‰‰st‰‰n t‰nne saakka, ympyr‰ ja jana leikkaavat (tai ovat sis‰kk‰in)
	Return True
End Function

Function Min#(a#, b#)
	If a < b Then
		Return a
	Else
		Return b
	EndIf
End Function

Function Max#(a#, b#)
	If a > b Then
		Return a
	Else
		Return b
	EndIf
End Function

Function point_direction#(x1#,z1#,x2#,z2#)
	Local dx#, dz#
	dx = x1 - x2
	dz = z1 - z2
	Return ATan2(dz,dx)
End Function

Function point_distance#(x1#,z1#,x2#,z2#)
	Local dx#,dy#
	dx = x1 - x2
	dy = z1 - z2
	Return Sqr((dx*dx)+(dy*dy)) 
End Function

Function angleDist#(a0#,a1#)
	Local b# = a0-a1
	Local bb#
	If b<-180.0 Then
		bb = b+360.0
	Else If b>180.0 Then
		bb = b-360.0
	Else
		bb = b
	EndIf
	Return bb
End Function

;--------------------------------------- decals -------------------------------------------------------

Type Decals
	Field obj%
	Field SizeChange#, Size#, MaxSize#
	Field AlphaChange#, Alpha#
	Field blendmode%
	Field fx%
	Field ID%
	Field Timer#
	
	Field lifetime#
	
	Field x#, y#, z#
	Field pitch#, yaw#, roll#
End Type

Function CreateDecal.Decals(id%, x#, y#, z#, pitch#, yaw#, roll#)
	Local d.Decals = New Decals
	
	d\x = x
	d\y = y
	d\z = z
	d\pitch = pitch
	d\yaw = yaw
	d\roll = roll
	
	d\MaxSize = 1.0
	
	d\Alpha = 1.0
	d\Size = 1.0
	d\obj = CreateSprite()
	d\blendmode = 1
	
	EntityTexture(d\obj, DecalTextures(id))
	EntityFX(d\obj, 0)
	SpriteViewMode(d\obj, 2)
	PositionEntity(d\obj, x, y, z)
	RotateEntity(d\obj, pitch, yaw, roll)
	
	d\ID = id
	
	If DecalTextures(id) = 0 Or d\obj = 0 Then Return Null
	
	Return d
End Function

Function UpdateDecals()
	Local d.Decals
	For d.Decals = Each Decals
		If d\SizeChange <> 0 Then
			d\Size=d\Size + d\SizeChange * FPSfactor
			ScaleSprite(d\obj, d\Size, d\Size)
			
			Select d\ID
				Case 0
					If d\Timer <= 0 Then
						Local angle# = Rand(360)
						Local temp# = Rnd(d\Size)
						Local d2.Decals = CreateDecal(1, EntityX(d\obj) + Cos(angle) * temp, EntityY(d\obj) - 0.0005, EntityZ(d\obj) + Sin(angle) * temp, EntityPitch(d\obj), Rnd(360), EntityRoll(d\obj))
						d2\Size = Rnd(0.1, 0.5) : ScaleSprite(d2\obj, d2\Size, d2\Size)
						PlaySound2(DecaySFX(Rand(1, 3)), Camera, d2\obj, 10.0, Rnd(0.1, 0.5))
						;d\Timer = d\Timer + Rand(50,150)
						d\Timer = Rand(50, 100)
					Else
						d\Timer= d\Timer-FPSfactor
					End If
				;Case 6
				;	EntityBlend d\obj, 2
			End Select
			
			If d\Size >= d\MaxSize Then d\SizeChange = 0 : d\Size = d\MaxSize
		End If
		
		If d\AlphaChange <> 0 Then
			d\Alpha = Min(d\Alpha + FPSfactor * d\AlphaChange, 1.0)
			EntityAlpha(d\obj, d\Alpha)
		End If
		
		If d\lifetime > 0 Then
			d\lifetime=Max(d\lifetime-FPSfactor,5)
		EndIf
		
		If d\Size <= 0 Or d\Alpha <= 0 Or d\lifetime=5.0  Then
			FreeEntity(d\obj)
			Delete d
		End If
	Next
End Function


;--------------------------------------- INI-functions -------------------------------------------------------

Type INIFile
	Field name$
	Field bank%
	Field bankOffset% = 0
	Field size%
End Type

Function ReadINILine$(file.INIFile)
	Local rdbyte%
	Local firstbyte% = True
	Local offset% = file\bankOffset
	Local bank% = file\bank
	Local retStr$ = ""
	rdbyte = PeekByte(bank,offset)
	While ((firstbyte) Or ((rdbyte<>13) And (rdbyte<>10))) And (offset<file\size)
		rdbyte = PeekByte(bank,offset)
		If ((rdbyte<>13) And (rdbyte<>10)) Then
			firstbyte = False
			retStr=retStr+Chr(rdbyte)
		EndIf
		offset=offset+1
	Wend
	file\bankOffset = offset
	Return retStr
End Function

Function UpdateINIFile$(filename$)
	Local file.INIFile = Null
	For k.INIFile = Each INIFile
		If k\name = Lower(filename) Then
			file = k
		EndIf
	Next
	
	If file=Null Then Return
	
	If file\bank<>0 Then FreeBank file\bank
	Local f% = ReadFile(file\name)
	Local fleSize% = 1
	While fleSize<FileSize(file\name)
		fleSize=fleSize*2
	Wend
	file\bank = CreateBank(fleSize)
	file\size = 0
	While Not Eof(f)
		PokeByte(file\bank,file\size,ReadByte(f))
		file\size=file\size+1
	Wend
	CloseFile(f)
End Function

Function GetINIString$(file$, section$, parameter$, defaultvalue$="")
	Local TemporaryString$ = ""
	
	Local lfile.INIFile = Null
	For k.INIFile = Each INIFile
		If k\name = Lower(file) Then
			lfile = k
		EndIf
	Next
	
	If lfile = Null Then
		DebugLog "CREATE BANK FOR "+file
		lfile = New INIFile
		lfile\name = Lower(file)
		lfile\bank = 0
		UpdateINIFile(lfile\name)
	EndIf
	
	lfile\bankOffset = 0
	
	section = Lower(section)
	
	;While Not Eof(f)
	While lfile\bankOffset<lfile\size
		Local strtemp$ = ReadINILine(lfile)
		If Left(strtemp,1) = "[" Then
			strtemp$ = Lower(strtemp)
			If Mid(strtemp, 2, Len(strtemp)-2)=section Then
				Repeat
					TemporaryString = ReadINILine(lfile)
					If Lower(Trim(Left(TemporaryString, Max(Instr(TemporaryString, "=") - 1, 0)))) = Lower(parameter) Then
						;CloseFile f
						Return Trim( Right(TemporaryString,Len(TemporaryString)-Instr(TemporaryString,"=")) )
					EndIf
				Until (Left(TemporaryString, 1) = "[") Or (lfile\bankOffset>=lfile\size)
				
				;CloseFile f
				Return defaultvalue
			EndIf
		EndIf
	Wend
	
	Return defaultvalue
End Function

Function GetINIInt%(file$, section$, parameter$, defaultvalue% = 0)
	Local txt$ = GetINIString(file$, section$, parameter$, defaultvalue)
	If Lower(txt) = "true" Then
		Return 1
	ElseIf Lower(txt) = "false"
		Return 0
	Else
		Return Int(txt)
	EndIf
End Function

Function GetINIFloat#(file$, section$, parameter$, defaultvalue# = 0.0)
	Return Float(GetINIString(file$, section$, parameter$, defaultvalue))
End Function


Function GetINIString2$(file$, start%, parameter$, defaultvalue$="")
	Local TemporaryString$ = ""
	Local f% = ReadFile(file)
	
	Local n%=0
	While Not Eof(f)
		Local strtemp$ = ReadLine(f)
		n=n+1
		If n=start Then 
			Repeat
				TemporaryString = ReadLine(f)
				If Lower(Trim(Left(TemporaryString, Max(Instr(TemporaryString, "=") - 1, 0)))) = Lower(parameter) Then
					CloseFile f
					Return Trim( Right(TemporaryString,Len(TemporaryString)-Instr(TemporaryString,"=")) )
				EndIf
			Until Left(TemporaryString, 1) = "[" Or Eof(f)
			CloseFile f
			Return defaultvalue
		EndIf
	Wend
	
	CloseFile f	
	
	Return defaultvalue
End Function

Function GetINIInt2%(file$, start%, parameter$, defaultvalue$="")
	Local txt$ = GetINIString2(file$, start%, parameter$, defaultvalue$)
	If Lower(txt) = "true" Then
		Return 1
	ElseIf Lower(txt) = "false"
		Return 0
	Else
		Return Int(txt)
	EndIf
End Function


Function GetINISectionLocation%(file$, section$)
	Local Temp%
	Local f% = ReadFile(file)
	
	section = Lower(section)
	
	Local n%=0
	While Not Eof(f)
		Local strtemp$ = ReadLine(f)
		n=n+1
		If Left(strtemp,1) = "[" Then
			strtemp$ = Lower(strtemp)
			Temp = Instr(strtemp, section)
			If Temp>0 Then
				If Mid(strtemp, Temp-1, 1)="[" Or Mid(strtemp, Temp-1, 1)="|" Then
					CloseFile f
					Return n
				EndIf
			EndIf
		EndIf
	Wend
	
	CloseFile f
End Function



Function PutINIValue%(file$, INI_sSection$, INI_sKey$, INI_sValue$)
	
	; Returns: True (Success) Or False (Failed)
	
	INI_sSection = "[" + Trim$(INI_sSection) + "]"
	Local INI_sUpperSection$ = Upper$(INI_sSection)
	INI_sKey = Trim$(INI_sKey)
	INI_sValue = Trim$(INI_sValue)
	Local INI_sFilename$ = file$
	
	; Retrieve the INI Data (If it exists)
	
	Local INI_sContents$ = INI_FileToString(INI_sFilename)
	
		; (Re)Create the INI file updating/adding the SECTION, KEY And VALUE
	
	Local INI_bWrittenKey% = False
	Local INI_bSectionFound% = False
	Local INI_sCurrentSection$ = ""
	
	Local INI_lFileHandle% = WriteFile(INI_sFilename)
	If INI_lFileHandle = 0 Then Return False ; Create file failed!
	
	Local INI_lOldPos% = 1
	Local INI_lPos% = Instr(INI_sContents, Chr$(0))
	
	While (INI_lPos <> 0)
		
		Local INI_sTemp$ = Mid$(INI_sContents, INI_lOldPos, (INI_lPos - INI_lOldPos))
		
		If (INI_sTemp <> "") Then
			
			If Left$(INI_sTemp, 1) = "[" And Right$(INI_sTemp, 1) = "]" Then
				
					; Process SECTION
				
				If (INI_sCurrentSection = INI_sUpperSection) And (INI_bWrittenKey = False) Then
					INI_bWrittenKey = INI_CreateKey(INI_lFileHandle, INI_sKey, INI_sValue)
				End If
				INI_sCurrentSection = Upper$(INI_CreateSection(INI_lFileHandle, INI_sTemp))
				If (INI_sCurrentSection = INI_sUpperSection) Then INI_bSectionFound = True
				
			Else
				If Left(INI_sTemp, 1) = ":" Then
					WriteLine INI_lFileHandle, INI_sTemp
				Else
						; KEY=VALUE				
					Local lEqualsPos% = Instr(INI_sTemp, "=")
					If (lEqualsPos <> 0) Then
						If (INI_sCurrentSection = INI_sUpperSection) And (Upper$(Trim$(Left$(INI_sTemp, (lEqualsPos - 1)))) = Upper$(INI_sKey)) Then
							If (INI_sValue <> "") Then INI_CreateKey INI_lFileHandle, INI_sKey, INI_sValue
							INI_bWrittenKey = True
						Else
							WriteLine INI_lFileHandle, INI_sTemp
						End If
					End If
				EndIf
				
			End If
			
		End If
		
			; Move through the INI file...
		
		INI_lOldPos = INI_lPos + 1
		INI_lPos% = Instr(INI_sContents, Chr$(0), INI_lOldPos)
		
	Wend
	
		; KEY wasn;t found in the INI file - Append a New SECTION If required And create our KEY=VALUE Line
	
	If (INI_bWrittenKey = False) Then
		If (INI_bSectionFound = False) Then INI_CreateSection INI_lFileHandle, INI_sSection
		INI_CreateKey INI_lFileHandle, INI_sKey, INI_sValue
	End If
	
	CloseFile INI_lFileHandle
	
	Return True ; Success
	
End Function

Function INI_FileToString$(INI_sFilename$)
	
	Local INI_sString$ = ""
	Local INI_lFileHandle%= ReadFile(INI_sFilename)
	If INI_lFileHandle <> 0 Then
		While Not(Eof(INI_lFileHandle))
			INI_sString = INI_sString + ReadLine$(INI_lFileHandle) + Chr$(0)
		Wend
		CloseFile INI_lFileHandle
	End If
	Return INI_sString
	
End Function

Function INI_CreateSection$(INI_lFileHandle%, INI_sNewSection$)
	
	If FilePos(INI_lFileHandle) <> 0 Then WriteLine INI_lFileHandle, "" ; Blank Line between sections
	WriteLine INI_lFileHandle, INI_sNewSection
	Return INI_sNewSection
	
End Function

Function INI_CreateKey%(INI_lFileHandle%, INI_sKey$, INI_sValue$)
	
	WriteLine INI_lFileHandle, INI_sKey + " = " + INI_sValue
	Return True
	
End Function

;--------------------------------------- MakeCollBox -functions -------------------------------------------------------


; Create a collision box For a mesh entity taking into account entity scale
; (will not work in non-uniform scaled space)
Function MakeCollBox(mesh%)
	Local sx# = EntityScaleX(mesh, 1)
	Local sy# = Max(EntityScaleY(mesh, 1), 0.001)
	Local sz# = EntityScaleZ(mesh, 1)
	GetMeshExtents(mesh)
	EntityBox mesh, Mesh_MinX * sx, Mesh_MinY * sy, Mesh_MinZ * sz, Mesh_MagX * sx, Mesh_MagY * sy, Mesh_MagZ * sz
End Function

; Find mesh extents
Function GetMeshExtents(Mesh%)
	Local s%, surf%, surfs%, v%, verts%, x#, y#, z#
	Local minx# = INFINITY
	Local miny# = INFINITY
	Local minz# = INFINITY
	Local maxx# = -INFINITY
	Local maxy# = -INFINITY
	Local maxz# = -INFINITY
	
	surfs = CountSurfaces(Mesh)
	
	For s = 1 To surfs
		surf = GetSurface(Mesh, s)
		verts = CountVertices(surf)
		
		For v = 0 To verts - 1
			x = VertexX(surf, v)
			y = VertexY(surf, v)
			z = VertexZ(surf, v)
			
			If (x < minx) Then minx = x
			If (x > maxx) Then maxx = x
			If (y < miny) Then miny = y
			If (y > maxy) Then maxy = y
			If (z < minz) Then minz = z
			If (z > maxz) Then maxz = z
		Next
	Next
	
	Mesh_MinX = minx
	Mesh_MinY = miny
	Mesh_MinZ = minz
	Mesh_MaxX = maxx
	Mesh_MaxY = maxy
	Mesh_MaxZ = maxz
	Mesh_MagX = maxx-minx
	Mesh_MagY = maxy-miny
	Mesh_MagZ = maxz-minz
	
End Function

Function EntityScaleX#(entity%, globl% = False)
	If globl Then TFormVector 1, 0, 0, entity, 0 Else TFormVector 1, 0, 0, entity, GetParent(entity)
	Return Sqr(TFormedX() * TFormedX() + TFormedY() * TFormedY() + TFormedZ() * TFormedZ())
End Function 

Function EntityScaleY#(entity%, globl% = False)
	If globl Then TFormVector 0, 1, 0, entity, 0 Else TFormVector 0, 1, 0, entity, GetParent(entity)
	Return Sqr(TFormedX() * TFormedX() + TFormedY() * TFormedY() + TFormedZ() * TFormedZ())
End Function 

Function EntityScaleZ#(entity%, globl% = False)
	If globl Then TFormVector 0, 0, 1, entity, 0 Else TFormVector 0, 0, 1, entity, GetParent(entity)
	Return Sqr(TFormedX() * TFormedX() + TFormedY() * TFormedY() + TFormedZ() * TFormedZ())
End Function 

Function Graphics3DExt%(width%,height%,depth%=32,mode%=2)
	If FE_InitExtFlag = 1 Then DeInitExt() ;prevent FastExt from breaking itself
	Graphics3D width,height,depth,mode
	InitExt()
	
	AntiAlias GetINIInt(OptionFile,"options","antialias")
	TextureAnisotropy% (GetINIInt(OptionFile,"options","anisotropy"),-1)
End Function

Function ResizeImage2(image%,width%,height%)
   img% = CreateImage(width,height)
   CopyRectStretch(0,0,ImageWidth(image),ImageHeight(image),0,0,width,height,ImageBuffer(image),ImageBuffer(img))
   FreeImage image
   Return img
End Function


Function RenderWorld2()
	CameraProjMode ark_blur_cam,0
	CameraProjMode Camera,1
	
	If WearingNightVision>0 Then
		AmbientLight Min(Brightness*2,255), Min(Brightness*2,255), Min(Brightness*2,255)
	ElseIf PlayerRoom<>Null
		If (PlayerRoom\RoomTemplate\Name<>"173") And (PlayerRoom\RoomTemplate\Name<>"exit1") And (PlayerRoom\RoomTemplate\Name<>"gatea") Then
			AmbientLight Brightness, Brightness, Brightness
		EndIf
	EndIf
	
	Local hasBattery% = 2
	If (WearingNightVision=1) Then ;fake a low-res display
		
		;hasBattery% = True
		
		For i=0 To MaxItemAmount-1
			If (Inventory(i)<>Null) Then
				If Inventory(i)\itemtemplate\tempname="nvgoggles" Then
					Inventory(i)\state=Inventory(i)\state-(FPSfactor*0.02)
					If Inventory(i)\state<=0.0 Then ;this nvg can't be used
						hasBattery = 0
						Msg = "The Night Vision Goggles need new batteries"
						BlinkTimer = -1.0
						MsgTimer = 350
						Exit
					ElseIf Inventory(i)\state<=100.0 Then
						hasBattery = 1
					EndIf
					
				EndIf
			EndIf
		Next
		If hasBattery Then
			CameraViewport Camera,1024.0-(GraphicWidth/8),1024.0-(GraphicHeight/8),GraphicWidth/4,GraphicHeight/4
			RenderWorldToTexture()
			;TextureAnisotropy(-1, -1) ;uncomment this to disable filtering on the low-res display
			CameraProjMode Camera,0
			ScaleRender(0.0,0.0,6.4*1280.0/GraphicWidth,6.4*1280.0/GraphicWidth)
			CameraProjMode Camera,1
				;TextureAnisotropy(0, -1) ;uncomment this to re-enable filtering if it's disabled
			CameraViewport Camera,0,0,GraphicWidth,GraphicHeight
		EndIf
	Else
		RenderWorld()
	EndIf
	
	If WearingNightVision=2 Then ;show a HUD
		NVTimer=NVTimer-FPSfactor
		
		If NVTimer<=0.0 Then
			For np.NPCs = Each NPCs
				np\NVX = EntityX(np\Collider,True)
				np\NVY = EntityY(np\Collider,True)
				np\NVZ = EntityZ(np\Collider,True)
			Next
			NVTimer = 600.0
		EndIf
		
		Color 255,255,255
		
		SetFont Font3
		
		Text GraphicWidth/2,20*MenuScale,"REFRESHING DATA IN",True,False
		
		Text GraphicWidth/2,60*MenuScale,f2s(NVTimer/60.0,1),True,False
		Text GraphicWidth/2,100*MenuScale,"SECONDS",True,False
		
		temp% = CreatePivot() : temp2% = CreatePivot()
		PositionEntity temp, EntityX(Collider), EntityY(Collider), EntityZ(Collider)
		
		Color 255,255,255;*(NVTimer/600.0)
		
		For np.NPCs = Each NPCs
			If np\NVName<>"" Then ;don't waste your time if the string is empty
				PositionEntity temp2,np\NVX,np\NVY,np\NVZ
				dist# = EntityDistance(temp2,Collider)
				If dist<23.5 Then ;don't draw text if the NPC is too far away
					PointEntity temp, temp2
					yawvalue# = WrapAngle(EntityYaw(Camera) - EntityYaw(temp))
					xvalue# = 0.0
					If yawvalue > 90 And yawvalue <= 180 Then
						xvalue# = Sin(90)/90*yawvalue
					Else If yawvalue > 180 And yawvalue < 270 Then
						xvalue# = Sin(270)/yawvalue*270
					Else
						xvalue = Sin(yawvalue)
					EndIf
					pitchvalue# = WrapAngle(EntityPitch(Camera) - EntityPitch(temp))
					yvalue# = 0.0
					If pitchvalue > 90 And pitchvalue <= 180 Then
						yvalue# = Sin(90)/90*pitchvalue
					Else If pitchvalue > 180 And pitchvalue < 270 Then
						yvalue# = Sin(270)/pitchvalue*270
					Else
						yvalue# = Sin(pitchvalue)
					EndIf
					
					Text GraphicWidth / 2 + xvalue * (GraphicWidth / 2),GraphicHeight / 2 - yvalue * (GraphicHeight / 2),np\NVName,True,True
					Text GraphicWidth / 2 + xvalue * (GraphicWidth / 2),GraphicHeight / 2 - yvalue * (GraphicHeight / 2) + 30.0 * MenuScale,f2s(dist,1)+" m",True,True
				EndIf
			EndIf
		Next
		
		FreeEntity (temp) : FreeEntity (temp2)
		
		Color 255,255,255
	EndIf
	
	;render sprites
	CameraProjMode ark_blur_cam,2
	CameraProjMode Camera,0
	RenderWorld()
	CameraProjMode ark_blur_cam,0
	
	If (WearingNightVision=1) And (hasBattery=1) And ((MilliSecs() Mod 800) < 400) Then
		Color 255,0,0
		SetFont Font3
		
		Text GraphicWidth/2,20*MenuScale,"WARNING: LOW BATTERY",True,False
		Color 255,255,255
	EndIf
End Function


Function ScaleRender(x#,y#,hscale#=1.0,vscale#=1.0)
	ShowEntity fresize_image
	ScaleEntity fresize_image,hscale,vscale,1.0
	PositionEntity fresize_image, x, y, 1.0001
	ShowEntity fresize_cam
	RenderWorld()
	HideEntity fresize_cam
	HideEntity fresize_image
End Function

Function InitFastResize()
   ;Create Camera
	Local cam% = CreateCamera()
	CameraProjMode cam, 2
	CameraZoom cam, 0.1
	CameraClsMode cam, 0, 0
	CameraRange cam, 0.1, 1.5
	MoveEntity cam, 0, 0, -10000
	fresize_cam = cam
	
   ;ark_sw = GraphicsWidth()
   ;ark_sh = GraphicsHeight()
	
   ;Create sprite
	Local spr% = CreateMesh(cam)
	Local sf% = CreateSurface(spr)
	AddVertex sf, -1, 1, 0, 0, 0
	AddVertex sf, 1, 1, 0, 1, 0
	AddVertex sf, -1, -1, 0, 0, 1
	AddVertex sf, 1, -1, 0, 1, 1
	AddTriangle sf, 0, 1, 2
	AddTriangle sf, 3, 2, 1
	EntityFX spr, 17
	ScaleEntity spr, 2048.0 / Float(GraphicWidth), 2048.0 / Float(GraphicHeight), 1
	PositionEntity spr, 0, 0, 1.0001
	EntityOrder spr, -100001
	EntityBlend spr, 1
	fresize_image = spr
	
   ;Create texture
	fresize_texture = CreateTexture(2048, 2048, 1+256+FE_RENDER+FE_ZRENDER)
	;TextureAnisotropy(fresize_texture)
	EntityTexture spr, fresize_texture
	
	HideEntity fresize_cam
End Function

Function RenderWorldToTexture()
	
   ;EntityAlpha ark_blur_image, 1.0
	HideEntity fresize_image
	old_buffer% = GetBuffer()
	SetBuffer(TextureBuffer(fresize_texture))
	RenderWorld()
	SetBuffer(old_buffer)
   ;CopyRect ark_sw / 2 - 1024, ark_sh / 2 - 1024, 2048, 2048, 0, 0, BackBuffer(), TextureBuffer(ark_blur_texture)
   ;CopyRect 0, 0, GraphicWidth, GraphicHeight, 1024.0 - GraphicWidth/2, 1024.0 - GraphicHeight/2, BackBuffer(), TextureBuffer(ark_blur_texture)
	
End Function




Function UpdateScreenGamma()
	Local n# = 1.0/ScreenGamma
	Local k%
	
	For k=0 To 255
		Local c# = Min(Max(0, ((k/255.0)^n)*255), 255)
		SetGamma k,k,k,c,c,c
	Next
	UpdateGamma
End Function

Function ManipulateNPCBones()
	Local n.NPCs,bone%,pvt%,pitch#,yaw#,roll#
	
	For n = Each NPCs
		If n\ManipulateBone
			bone% = FindChild(n\obj,n\BoneToManipulate$)
			If bone% = 0 Then RuntimeError "ERROR: NPC bone "+Chr(34)+n\BoneToManipulate+Chr(34)+" is not existing!"
			Select n\ManipulationType
				Case 0 ;<--- looking at player
					PointEntity bone%,Camera
					RotateEntity bone%,EntityPitch(bone%),20,0
				Case 1 ;<--- looking at player #2
					PointEntity bone%,Collider
					RotateEntity bone%,0,EntityYaw(bone%),0
			End Select
		EndIf
	Next
	
End Function

Function Inverse#(number#)
	
	Return 1.0-number#
	
End Function
;~IDEal Editor Parameters:
;~F#91#111#115#11C#36C#46F#48D#503#510#5A4#61B#632#63F#671#718#7EC#132E#14D1#164E#166D
;~F#168C#16AA#16AE#16CE
;~C#Blitz3D