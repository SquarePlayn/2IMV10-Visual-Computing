#version 400 core

in vec3 color;
in vec3 surfaceNormal;
in vec3 toLightVector;
in vec2 flatPosition;

out vec4 out_Color;

uniform vec3 lightColor;
uniform sampler2D textureSampler;
uniform bool textureAvailable;
uniform float border;

const float MIN_BRIGHTNESS = 0.3;

void main(void) {
    vec3 unitNormal = normalize(surfaceNormal);
    vec3 unitLightVector = normalize(toLightVector);

    // Diffuse lighting
    float nDot1 = dot(unitNormal, unitLightVector);
    float brightness = max(nDot1, MIN_BRIGHTNESS);
    vec3 diffuse = brightness * lightColor;

    if (textureAvailable) {
        vec2 texPos = (flatPosition + border) / (100 + 2*border);
        vec4 texColor = texture2D(textureSampler, clamp(texPos, 0.0, 1.0));
        out_Color = vec4(diffuse, 1.0) * texColor;
    } else {
        out_Color = vec4(diffuse, 1.0) * vec4(color, 1.0);
    }
    
}
