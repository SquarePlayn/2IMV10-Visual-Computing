#version 400 core

in vec3 position;
in vec3 in_color;
in vec3 normal;

out vec3 color;
out vec3 surfaceNormal;
out vec3 toLightVector;
out vec2 flatPosition;

uniform mat4 transformationMatrix;
uniform mat4 projectionMatrix;
uniform mat4 viewMatrix;
uniform vec3 lightPosition;
uniform vec3 cameraPosition;
uniform float time;

const float PI = 3.14159265359;

void main(void) {
    flatPosition = position.xz;

    // Transform the vertex position according to entity movement and rotation
    vec4 transPosition = transformationMatrix * vec4(position, 1.0);

    // Vector pointing from vertex to light
    // Light position is already in globe space
    toLightVector = lightPosition - transPosition.xyz;

    // Final position of this vertex on the screen
    gl_Position = projectionMatrix * viewMatrix * transPosition;

    // normal
    surfaceNormal = (vec4(normal, 1.0)).xyz; //(transformationMatrix * vec4(normal, 1.0)).xyz;

    // color
    color = vec3(1, 1, 1);
}