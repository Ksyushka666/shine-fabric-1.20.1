#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform float ShineEntityStrength;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
in vec2 texCoord1;
in vec4 normal;

out vec4 fragColor;
layout(location = 1) out vec4 bloomColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
    bloomColor = vec4(fragColor.rgb * fragColor.a, ShineEntityStrength);
}
