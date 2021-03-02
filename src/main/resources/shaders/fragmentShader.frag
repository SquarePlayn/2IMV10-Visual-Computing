#version 400 core

in vec3 color;
in vec3 surfaceNormal;
in vec3 toLightVector;

out vec4 out_Color;

uniform vec3 lightColor;

const float MIN_BRIGHTNESS = 0.3;

void main(void) {

    vec3 unitNormal = normalize(surfaceNormal);
    vec3 unitLightVector = normalize(toLightVector);

    // Diffuse lighting
    float nDot1 = dot(unitNormal, unitLightVector);
    float brightness = max(nDot1, MIN_BRIGHTNESS);
    vec3 diffuse = brightness * lightColor;

    out_Color = vec4(diffuse, 1.0) * vec4(color, 1.0);

    out_Color = vec4(0.5, 0.5, 0.5, 1.0);

    // Uncomment to display normals for debugging
    //out_Color = vec4(surfaceNormal, 1.0);

}