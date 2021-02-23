#version 400 core

in vec3 position;
in vec3 in_color;
in vec3 normal;
in float in_reflectiveness;

out vec3 color;
out vec3 surfaceNormal;
out vec3 toLightVector;
out vec3 reflectedVector;
out float reflectiveness;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform vec3 lightPosition;
uniform vec3 cameraPosition;
uniform float time;

const float PI = 3.14159265359;

void main(void) {

    // Transform the vertex position according to entity movement and rotation
    vec3 transPosition = (transformationMatrix * vec4(position, 1.0)).xyz;

    // Vector pointing from vertex to light
    // Light position is already in globe space
    toLightVector = lightPosition - transPosition.xyz;

    // Final position of this vertex on the screen
    gl_Position = projectionMatrix * viewMatrix * vec4(transPosition, 1.0);

    // normal
    surfaceNormal = (transformationMatrix * vec4(normal, 0.0)).xyz;

    if (in_reflectiveness != 0) { // Only calculate reflection if vertex should have reflections
        // Calculate reflected vector. We first calculate the vector from camera to the vertex and use this to calculate
        // the reflected vector.
        vec3 unitNormal = normalize(surfaceNormal);
        vec3 viewVector = normalize(transPosition.xyz - cameraPosition);
        reflectedVector = reflect(viewVector, unitNormal); // GLSL's built-in reflect() function
        reflectedVector *= vec3(1.0, -1.0, 1.0);
    } else {
        reflectedVector = vec3(0, 0, 0); // If not reflective, pass something arbitrary
    }

    color = in_color;

    reflectiveness = in_reflectiveness;
}