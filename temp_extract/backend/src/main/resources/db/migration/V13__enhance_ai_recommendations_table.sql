alter table ai_recommendations
    add constraint ai_recommendations_target_entity_type_not_blank
        check (length(trim(target_entity_type)) > 0),
    add constraint ai_recommendations_input_summary_not_blank
        check (length(trim(input_summary)) > 0),
    add constraint ai_recommendations_recommendation_not_blank
        check (length(trim(recommendation)) > 0),
    add constraint ai_recommendations_explanation_not_blank
        check (length(trim(explanation)) > 0),
    add constraint ai_recommendations_confidence_score_range
        check (confidence_score is null or (confidence_score >= 0 and confidence_score <= 100));

create index if not exists idx_ai_recommendations_type
    on ai_recommendations (recommendation_type);

create index if not exists idx_ai_recommendations_target
    on ai_recommendations (target_entity_type, target_entity_id);

create index if not exists idx_ai_recommendations_approved_by
    on ai_recommendations (approved_by_user_id);

create index if not exists idx_ai_recommendations_created_at
    on ai_recommendations (created_at);
