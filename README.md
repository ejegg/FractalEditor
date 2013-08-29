FractalEditor
=============

Android app to edit 3d fractals generated via iterated function systems (https://en.wikipedia.org/wiki/Iterated_function_system)

Each function in the app is represented as a 3d transform, with rotation, translation, and uniform or non-uniform scaling supported.  In edit mode, these are shown as cubes you can drag (long press to start dragging), rotate, and stretch.  In both edit and render mode, a fling gesture will set the camera orbiting around the fractal.  In render mode, if the camera is not moving, the app calculates more points for a while and layers them atop the existing points.  This is because most Android devices can't send enough points to the GPU to make a fractal look solid.

Comes with a Sierpinski pyramid, a spleenwort fern, and a Menger sponge pre-loaded.  

Buttons in main bar:
Load, Save, Undo
Add function: adds another cube, defaulting to no translation and 50% uniform scaling
Delete selected function
Scale mode toggle: switches from pinches scaling the selected cube in all directions equally, to only scaling along the axis of your pinch.
Edit mode toggle: switches between edit mode and render mode, where you get to see the results of the fractal.  

TODO:
Implement settings dialog (color, number of layers rendered on still screen)
Save fractal state and undo stack on pause/destroy, restore on re-entry
Look into JNI to speed up rendering
Create web backend to share fractals and view them in WebGL on desktop GPU
Use ZXing to share via QR code

I wrote an initial version of this while I was learning the basics of Android and 3d coding.  That one had no good separation of concerns and started getting crashy after my phone updated to 4.1.1, so I rewrote it from scratch, pulling in just the good bits from the old version.
