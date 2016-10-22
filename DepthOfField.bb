;
; Depth of field
;
; Created by Mikkel Fredborg
; Use as you please!
;

; Depth of Field setup
Type DepthOfField
	Field layers
	Field layer[99]
	Field texture
	Field tsize
	Field tbuffer
	Field near#,far#
	Field camera
End Type

Function DOF_Update(dof.DepthOfField,viewportX%=0,viewportY%=0)
	
	HideEntity dof\layer[0]
	
	CameraRange dof\camera,dof\near*0.95,1000
	CameraViewport dof\camera,0,0,dof\tsize,dof\tsize
	RenderWorld
	CopyRect 0,0,dof\tsize,dof\tsize,0,0,BackBuffer(),dof\tbuffer
	
	ShowEntity dof\layer[0]
	
	CameraRange dof\camera,0.1,1000
	
	If viewportX<>0 And viewportY<>0
		CameraViewport dof\camera,0,0,viewportX,viewportY
	Else
		CameraViewport dof\camera,0,0,GraphicsWidth(),GraphicsHeight()
	EndIf
	
End Function

Function DOF_Create.DepthOfField(camera,layers,spread#=0.0)
	
	Local DOF.DepthOfField = New DepthOfField
	
	DOF\camera = camera
	
	DOF\layers = layers
	
	DOF\tsize	 = DOF_TexSizeValue
	DOF\near	 = 100.0
	DOF\far		 = 300.0
	
	ClearTextureFilters
	DOF\texture = CreateTexture(DOF\tsize,DOF\tsize,1+256+16+32)
	DOF\tbuffer = TextureBuffer(DOF\texture)
	
	ang# = 360.0/Float(DOF\layers)
	For i = 0 To DOF\layers-1
		DOF\layer[i] = CreateFace(1)
		
		EntityAlpha DOF\layer[i],1.0/Float(DOF\layers)
		EntityFX	DOF\layer[i],1+8
		
		ps# = DOF\near+(i*((DOF\far-DOF\near)/Float(DOF\layers)))
		
		px# = Sin(i*ang)*(i/Float(DOF\layers))*spread
		py# = Cos(i*ang)*(i/Float(DOF\layers))*spread
		
		PositionEntity DOF\layer[i],px,py,ps
		ScaleEntity DOF\layer[i],ps,ps,1.0		
		
		EntityTexture DOF\layer[i],DOF\texture
		
		If i = 0
			EntityParent DOF\layer[i],DOF\camera,True
		Else
			EntityParent DOF\layer[i],DOF\layer[i-1],True
		End If
	Next
	
	DebugLog DOF\tsize
	
	Return DOF
	
End Function

Function CreateFace(segs=1,parent=0)
	
	mesh=CreateMesh( parent )
	surf=CreateSurface( mesh )
	stx#=-1.0
	sty#=stx
	stp#=Float(2)/Float(segs)
	y#=sty
	For a=0 To segs
		x#=stx
		v#=a/Float(segs)
		For b=0 To segs
			u#=b/Float(segs)
			AddVertex(surf,x,-y,0,u,v) ; swap these for a different start orientation
			x=x+stp
		Next
		y=y+stp
	Next
	For a=0 To segs-1
		For b=0 To segs-1
			v0=a*(segs+1)+b:v1=v0+1
			v2=(a+1)*(segs+1)+b+1:v3=v2-1
			AddTriangle( surf,v0,v2,v1 )
			AddTriangle( surf,v0,v3,v2 )
		Next
	Next
	
	FlipMesh mesh
	UpdateNormals mesh
	
	Return mesh
	
End Function

;Function added so the effect can be deleted manually instead only by the "ClearWorld" command
Function DeleteDOF(DOF.DepthOfField)
	Local i%
	
	FreeTexture DOF\texture
	DOF\texture = 0
	FreeEntity DOF\layer[0]
	DOF\layer[0] = 0
	
	Delete DOF
	
End Function


;~IDEal Editor Parameters:
;~F#8#12#27#55#78
;~C#Blitz3D