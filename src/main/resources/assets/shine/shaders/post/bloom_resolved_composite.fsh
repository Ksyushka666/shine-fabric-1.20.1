#version 330

uniform sampler2D MainSampler;
uniform sampler2D BloomSampler;
uniform sampler2D DepthSampler;
uniform sampler2D TerrainDepthSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 MainSize;
    vec2 BloomSize;
    vec2 DepthSize;
    vec2 TerrainDepthSize;
};

layout(std140) uniform BloomCompositeConfig {
    float Strength;
};

layout(std140) uniform BloomCompositeDistanceConfig {
    float MaxDistance;
    float NearPlane;
    float FarPlane;
    float DistanceFadeRange;
};

out vec4 fragColor;

float linearize_depth(float depth) {
    float zNdc = depth * 2.0 - 1.0;
    float denominator = FarPlane + NearPlane - zNdc * (FarPlane - NearPlane);
    return (2.0 * NearPlane * FarPlane) / max(denominator, 1.0e-6);
}

float distance_limit(float depth, float sceneDistance) {
    if (depth >= 0.9999) {
        return 1.0;
    }
    return 1.0 - smoothstep(MaxDistance, MaxDistance + DistanceFadeRange, sceneDistance);
}

void main() {
    vec2 uv = gl_FragCoord.xy / OutSize;
    vec4 sceneColor = texture(MainSampler, uv);
    float depth = texture(DepthSampler, uv).r;
    float terrainDepth = texture(TerrainDepthSampler, uv).r;
    float sceneDistance = linearize_depth(depth);
    float terrainDistance = linearize_depth(terrainDepth);
    float distanceMask = max(distance_limit(terrainDepth, terrainDistance), distance_limit(depth, sceneDistance));
    vec3 encodedBloom = texture(BloomSampler, uv).rgb;
    vec3 bloom = encodedBloom * encodedBloom * Strength * distanceMask;
    fragColor = vec4(sceneColor.rgb + bloom, sceneColor.a);
}
