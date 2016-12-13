;
;
; Textured spotlight thingy
;
; Created by Mikkel Fredborg
; Use as you please!
;

Type dl_receiver
	Field mesh
End Type

Type dl_light
	Field entity
	Field range#
	Field scale#
	Field intensity#
	Field flicker#
	Field flickerrange#
	Field r#,g#,b#
End Type

Global dl_brush
Global dl_tex

Global BigRoomMesh

Function DL_Init()

	;ClearTextureFilters
	;dl_tex = LoadTexture("..\GFX\player\flashlightblur.png",1+16+32)
	dl_tex = LoadTexture("GFX\flashlightblur.png",1+2+16+32+256)
	
	dl_brush = CreateBrush()
	;BrushBlend dl_brush,3
	BrushFX dl_brush,1+2
	BrushTexture dl_brush,dl_tex
	
	BigRoomMesh = CreateMesh()

End Function

Function DL_Free()
	Local dlr.dl_receiver, dll.dl_light

	For dlr.dl_receiver = Each dl_receiver
		;If dlr\mesh<>0 Then FreeEntity dlr\mesh
		Delete dlr
	Next

	For dll.dl_light = Each dl_light
		Delete dll
	Next

	;If dl_tex	Then FreeTexture dl_tex
	;If dl_brush	Then FreeBrush dl_brush

	dl_tex		= 0
	dl_brush	= 0

End Function

Function DL_SetReceiver(mesh,scaleX#=1.0,scaleY#=1.0,scalez#=1.0)
	Local dlr.dl_receiver

	dlr.dl_receiver = New dl_receiver
	dlr\mesh = CopyMesh(mesh,mesh)
	;ScaleEntity dlr\mesh,scaleX#,scaleY#,scalez#
	;PositionEntity dlr\mesh,EntityX(mesh),EntityY(mesh),EntityZ(mesh)
	PaintMesh dlr\mesh,dl_brush
	
End Function

Function DL_SetLight(entity,range#=500.0,scale#=0.75,intensity#=2.0,flicker#=0.05,flickerrange#=0.5,r#=200,g#=220,b#=255)
	Local dll.dl_light

	dll.dl_light = First dl_light

	If dll = Null
		dll.dl_light = New dl_light
	End If
	
	dll\entity 	 = entity
	dll\range  	 = range
	dll\scale		= scale
	dll\intensity = intensity
	dll\flicker	 = flicker
	dll\flickerrange = flickerrange
	
	dll\r		= r
	dll\g		= g
	dll\b		= b
	
End Function

Function DL_Update()
	Local dll.dl_light, dlr.dl_receiver, intensity#, mesh, n_surfs, s, surf, n_verts, v, x#, y#, z#, dist#, falloff#, dot#

	dll.dl_light = First dl_light
	If dll = Null Then Return

	If Rnd(0.0,1.0)<dll\flicker
		intensity# = dll\intensity*Rnd(dll\flickerrange,1.0)
	Else
		intensity# = dll\intensity
	End If

	For dlr.dl_receiver = Each dl_receiver
		If dlr\mesh <> 0
			;If EntityDistance(dlr\mesh,Camera)<10.0
				mesh = dlr\mesh
				n_surfs = CountSurfaces(mesh)
				For s = 1 To n_surfs
					surf = GetSurface(mesh,s)
					n_verts = CountVertices(surf)-1
					For v = 0 To n_verts
						TFormPoint VertexX(surf,v),VertexY(surf,v),VertexZ(surf,v),mesh,dll\entity
						x# = TFormedX()
						y# = TFormedY()
						z# = TFormedZ()
						
						dist# = Sqr(x*x + y*y + z*z)*dll\scale
						;;tu# = (x/dist)+0.5
						;;tv# = 1.0-((y/dist)+0.5)
						
						;;VertexTexCoords surf,v,tu,tv
						VertexTexCoords surf,v,((TFormedX()/5)+0.5),(1-((TFormedY()/5)+.5))
						
						If z>dll\range Then z = dll\range
						falloff# = 1.0-(z/dll\range)
						If falloff<0.0 Then falloff = 0.0
						If falloff>1.0 Then falloff = 1.0
						
						TFormNormal VertexNX(surf,v),VertexNY(surf,v),VertexNZ(surf,v),mesh,dll\entity
						dot# = -TFormedZ()*falloff*intensity
						If dot>0.0
							VertexColor surf,v,dot*dll\r,dot*dll\g,dot*dll\b
						Else
							VertexColor surf,v,0,0,0
						EndIf
					Next
				Next
			;EndIf
		EndIf
	Next

End Function


;~IDEal Editor Parameters:
;~F#8#C#1B#2A#3E#49#5F
;~C#Blitz3D