;Graphics3D 1280,720,0,2

Global AATextEnable% = GetINIInt(OptionFile, "options", "antialiased text")
Global AASelectedFont%
Global AATextCam%,AATextSprite%[150]
Global AACharW%,AACharH%

Global AACamViewW%,AACamViewH%

Type AAFont
	Field texture%
	Field backup% ;images don't get erased by clearworld
	
	Field x%[128] ;not going to bother with unicode
	Field y%[128]
	Field w%[128]
	Field h%[128]
	
	Field lowResFont% ;for use on other buffers
	
	Field mW%
	Field mH%
	Field texH%
End Type

Function InitAAFont()
	If AATextEnable Then
		;Create Camera
		Local cam% = CreateCamera()
		CameraViewport cam,0,0,10,10;GraphicWidth,GraphicHeight
		;CameraProjMode cam, 2
		CameraZoom cam, 0.1
		CameraClsMode cam, 0, 0
		CameraRange cam, 0.1, 1.5
		MoveEntity cam, 0, 0, -20000
		AATextCam = cam
		CameraProjMode cam,0
	
	    ;Create sprite
		Local spr% = CreateMesh(cam)
		Local sf% = CreateSurface(spr)
		AddVertex sf, -1, 1, 0, 0, 0 ;vertex 0; uv:0,0
		AddVertex sf, 1, 1, 0, 1, 0  ;vertex 1; uv:1,0
		AddVertex sf, -1, -1, 0, 0, 1;vertex 2; uv:0,1
		AddVertex sf, 1, -1, 0, 1, 1 ;vertex 3; uv:1,1
		AddTriangle sf, 0, 1, 2
		AddTriangle sf, 3, 2, 1
		EntityFX spr, 17+32
		PositionEntity spr, 0, 0, 1.0001
		EntityOrder spr, -100001
		EntityBlend spr, 1
		AATextSprite[0] = spr : HideEntity AATextSprite[0]
		For i%=1 To 149
			spr = CopyMesh(AATextSprite[0],cam)
			EntityFX spr, 17+32
			PositionEntity spr, 0, 0, 1.0001
			EntityOrder spr, -100001
			EntityBlend spr, 1
			AATextSprite[i] = spr : HideEntity AATextSprite[i]
		Next
	EndIf
End Function

Function AASpritePosition(ind%,x%,y%)
	;THE HORROR
	Local nx# = (((Float(x-(AACamViewW/2))/Float(AACamViewW))*2))
	Local ny# = -(((Float(y-(AACamViewH/2))/Float(AACamViewW))*2))
	
	;how does this work pls help
	nx = nx-((1.0/Float(AACamViewW))*(((AACharW-2) Mod 2)))+(1.0/Float(AACamViewW))
	ny = ny-((1.0/Float(AACamViewW))*(((AACharH-2) Mod 2)))+(1.0/Float(AACamViewW))
	
	PositionEntity AATextSprite[ind],nx,ny,1.0
End Function

Function AASpriteScale(ind%,w%,h%)
	ScaleEntity AATextSprite[ind],1.0/Float(AACamViewW)*Float(w), 1/Float(AACamViewW)*Float(h), 1
	AACharW = w : AACharH = h
End Function

Function ReloadAAFont() ;CALL ONLY AFTER CLEARWORLD
	If AATextEnable Then
		InitAAFont()
		For font.AAFont = Each AAFont
			font\texture = CreateTexture(1024,1024,3)
			LockBuffer ImageBuffer(font\backup)
			LockBuffer TextureBuffer(font\texture)
			For ix%=0 To 1023
				For iy%=0 To font\texH
					px% = ReadPixelFast(ix,iy,ImageBuffer(font\backup)) Shl 24
					WritePixelFast(ix,iy,$FFFFFF+px,TextureBuffer(font\texture))
				Next
			Next
			UnlockBuffer TextureBuffer(font\texture)
			UnlockBuffer ImageBuffer(font\backup)
		Next
	EndIf
End Function

Function AASetFont(fnt%)
	AASelectedFont = fnt
	Local font.AAFont = Object.AAFont(AASelectedFont)
	If AATextEnable Then
		For i%=0 To 149
			EntityTexture AATextSprite[i],font\texture
		Next
	EndIf
End Function

Function AAStringWidth%(txt$)
	Local font.AAFont = Object.AAFont(AASelectedFont)
	If (AATextEnable) Then
		Local retVal%=0
		For i=1 To Len(txt)
			Local char% = Asc(Mid(txt,i,1))
			If char>=0 And char<=127 Then
				retVal=retVal+font\w[char]-2
			EndIf
		Next
		Return retVal+2
	Else
		SetFont font\lowResFont
		Return StringWidth(txt)
	EndIf
End Function

Function AAStringHeight%(txt$)
	Local font.AAFont = Object.AAFont(AASelectedFont)
	If (AATextEnable) Then
		Return font\mH
	Else
		SetFont font\lowResFont
		Return StringHeight(txt)
	EndIf
End Function

Function AAText(x%,y%,txt$,cx%=False,cy%=False,a#=1.0)
	Local font.AAFont = Object.AAFont(AASelectedFont)
	
	If (GraphicsBuffer()<>BackBuffer()) Or (Not AATextEnable) Then
		SetFont font\lowResFont
		Local oldr% = ColorRed() : Local oldg% = ColorGreen() : Local oldb% = ColorBlue()
		Color oldr*a,oldg*a,oldb*a
		Text x,y,txt,cx,cy
		Color oldr,oldg,oldb
		Return
	EndIf
	
	If cx Then
		x=x-(AAStringWidth(txt)/2)
	EndIf
	
	If cy Then
		y=y-(AAStringHeight(txt)/2)
	EndIf
	
	If Camera<>0 Then HideEntity Camera
	If ark_blur_cam<>0 Then HideEntity ark_blur_cam
	
	Local tX% = 0
	CameraProjMode AATextCam,2
	
	Local char%
	
	Local tw%=0
	For i=1 To Len(txt)
		char = Asc(Mid(txt,i,1))
		If char>=0 And char<=127 Then
			tw=tw+font\w[char]
		EndIf
	Next
	
	AACamViewW = tw
	AACamViewW = AACamViewW+(AACamViewW Mod 2)
	AACamViewH = AAStringHeight(txt)
	AACamViewH = AACamViewH+(AACamViewH Mod 2)
	CameraViewport AATextCam,x,y,AACamViewW,AACamViewH
	For i=1 To Len(txt)
		EntityAlpha AATextSprite[i-1],a
		EntityColor AATextSprite[i-1],ColorRed(),ColorGreen(),ColorBlue()
		ShowEntity AATextSprite[i-1]
		char% = Asc(Mid(txt,i,1))
		If char>=0 And char<=127 Then
			AASpriteScale(i-1,font\w[char],font\h[char])
			AASpritePosition(i-1,tX+(font\w[char]/2),(font\h[char]/2))
			VertexTexCoords GetSurface(AATextSprite[i-1],1),0,Float(font\x[char])/1024.0,Float(font\y[char])/1024.0
			VertexTexCoords GetSurface(AATextSprite[i-1],1),1,Float(font\x[char]+font\w[char])/1024.0,Float(font\y[char])/1024.0
			VertexTexCoords GetSurface(AATextSprite[i-1],1),2,Float(font\x[char])/1024.0,Float(font\y[char]+font\h[char])/1024.0
			VertexTexCoords GetSurface(AATextSprite[i-1],1),3,Float(font\x[char]+font\w[char])/1024.0,Float(font\y[char]+font\h[char])/1024.0
			tX = tX+font\w[char]-2
		EndIf
	Next
	RenderWorld
	CameraProjMode AATextCam,0
	
	For i=1 To Len(txt)
		HideEntity AATextSprite[i-1]
	Next
	
	If Camera<>0 Then ShowEntity Camera
	If ark_blur_cam<>0 Then ShowEntity ark_blur_cam
End Function

Function AALoadFont%(file$="Tahoma", height=13, bold=0, italic=0, underline=0, AATextScaleFactor%=2)
	Local newFont.AAFont = New AAFont
	
	newFont\lowResFont = LoadFont(file,height,bold,italic,underline)
	
	newFont\mW = (FontWidth()/AATextScaleFactor)+3
	newFont\mH = (FontHeight()/AATextScaleFactor)+3
	
	If AATextEnable Then
		Local hResFont% = LoadFont(file,height*AATextScaleFactor,bold,italic,underline)
		Local tImage% = CreateTexture(1024,1024,3)
		Local tX% = 0 : Local tY% = 1
		SetFont hResFont
		
		newFont\mW = (FontWidth()/AATextScaleFactor)+3
		newFont\mH = (FontHeight()/AATextScaleFactor)+3
		
		Local tCharImage% = CreateImage(FontWidth()+2*AATextScaleFactor,FontHeight()+2*AATextScaleFactor)
		ClsColor 0,0,0
		LockBuffer TextureBuffer(tImage)
		
		Local miy% = newFont\mH*((newFont\mW*95/1024)+2)
		DebugLog miy
		
		newFont\mW = 0
		
		For ix%=0 To 1023
			For iy%=0 To miy
				WritePixelFast(ix,iy,$FFFFFF,TextureBuffer(tImage))
			Next
		Next
		
		For i=32 To 126
			SetBuffer ImageBuffer(tCharImage)
			Cls
			;Color 60,60,60
			;Text 0,0,Chr(i)
			;Text AATextScaleFactor,0,Chr(i)
			;Text 0,AATextScaleFactor,Chr(i)
			;Text AATextScaleFactor,AATextScaleFactor,Chr(i)
			Color 255,255,255
			Text AATextScaleFactor/2,AATextScaleFactor/2,Chr(i)
			Local tw% = StringWidth(Chr(i)) : Local th% = FontHeight()
			SetBuffer BackBuffer()
				
			LockBuffer ImageBuffer(tCharImage)
			For iy%=0 To th/Float(AATextScaleFactor)+2*AATextScaleFactor
				For ix%=0 To tw/Float(AATextScaleFactor)+2*AATextScaleFactor
					Local rx% = ix*AATextScaleFactor
					Local ry% = iy*AATextScaleFactor
					Local ar% = 0
					For iiy%=ry To ry+AATextScaleFactor/2
						If (iiy>=0 And iiy<th+1*AATextScaleFactor) Then
							For iix%=rx To rx+AATextScaleFactor/2
								If (iix>=0 And iix<tw+1*AATextScaleFactor) Then
									ar=ar+((ReadPixelFast(iix,iiy,ImageBuffer(tCharImage)) And $FF)/Float(AATextScaleFactor+3)*1.25)
									ar=((Float(ar)/255.0)^(0.9))*255
									If ar>255 Then ar=255
								EndIf
							Next
						EndIf
					Next
					;ar = ((Float(ar)/255.0)^0.5)*255.0
					WritePixelFast(ix+tX,iy+tY,$FFFFFF+(ar Shl 24),TextureBuffer(tImage))
				Next
			Next
			UnlockBuffer ImageBuffer(tCharImage)
	
			newFont\x[i]=tX
			newFont\y[i]=tY
			newFont\w[i]=(StringWidth(Chr(i))/AATextScaleFactor)+3
			
			If newFont\mW<newFont\w[i]-3 Then newFont\mW=newFont\w[i]-3
			
			newFont\h[i]=(FontHeight()/AATextScaleFactor)+3
			tX=tX+newFont\w[i]+2
			If (tX>1024-(FontWidth()/AATextScaleFactor)-4) Then
				tX=0
				tY=tY+(FontHeight()/AATextScaleFactor)+6
			EndIf
		Next
		
		newFont\texH = miy
		
		Local backup% = CreateImage(1024,1024)
		LockBuffer ImageBuffer(backup)
		For ix%=0 To 1023
			For iy%=0 To newFont\texH
				px% = ReadPixelFast(ix,iy,TextureBuffer(tImage)) Shr 24
				px=px+(px Shl 8)+(px Shl 16)
				WritePixelFast(ix,iy,$FF000000+px,ImageBuffer(backup))
			Next
		Next
		UnlockBuffer ImageBuffer(backup)
		;SaveImage backup,file+".bmp"
		newFont\backup = backup
		
		UnlockBuffer TextureBuffer(tImage)
		
		
		FreeImage tCharImage
		FreeFont hResFont
		newFont\texture = tImage
	EndIf
	Return Handle(newFont)
End Function
